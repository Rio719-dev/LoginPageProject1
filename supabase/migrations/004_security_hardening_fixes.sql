-- Restores three protections that a live-database audit found were NOT actually
-- in effect, despite 001_secure_auth.sql stating they should be. This migration
-- re-asserts the intended state; it does not change the design.
--
-- 1. Column-level lockdown on public.profiles. Audit found `authenticated` (and
--    even `anon`) could still update role/requested_role/must_change_password/email,
--    meaning any signed-in user could PATCH their own profile row's `role` to
--    'super_admin' directly via PostgREST and self-promote. The RLS UPDATE policy
--    already allows `id = auth.uid()`, so column privileges were the only thing
--    meant to block this.
--
--    Root cause: 001_secure_auth.sql's `revoke update (col1, col2, ...) ... from
--    authenticated` only strips column-level grants. It did nothing here because
--    `grant select, insert, update on public.profiles to authenticated` (also in
--    001_secure_auth.sql, applied after the revoke in file order) is a *table-level*
--    UPDATE grant, and table-level UPDATE authorizes every column regardless of any
--    column-level revoke -- there is no column-level revoke that can narrow a
--    table-level grant. The column revoke was therefore a no-op the whole time.
--    Fixing this requires revoking table-level UPDATE entirely and granting UPDATE
--    only at the column level.
revoke update on public.profiles from authenticated;
revoke update on public.profiles from anon;
revoke update (role, requested_role, must_change_password, email) on public.profiles from authenticated, anon;
grant update (full_name, birthday, address, mobile) on public.profiles to authenticated;

-- 2. is_admin_or_super_admin() and the SELECT policy that depends on it. Audit
--    found the live database still had the old is_super_admin()-only SELECT
--    policy and never had this function at all, so Admin accounts could only
--    read their own profile row and AdminMonitoringActivity showed nothing for
--    them (Super Admin was unaffected).
create or replace function public.is_admin_or_super_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.profiles
    where id = auth.uid() and role in ('admin', 'super_admin')
  );
$$;

drop policy if exists "users read own profile or super admins read all" on public.profiles;
drop policy if exists "users read own profile or admins and super admins read all" on public.profiles;
create policy "users read own profile or admins and super admins read all"
on public.profiles for select to authenticated
using (id = auth.uid() or public.is_admin_or_super_admin());

grant execute on function public.is_admin_or_super_admin() to authenticated;

-- 3. clear_login_failures_by_email() must only ever be callable by the
--    secure-login Edge Function's service_role key. Audit found it was still
--    reachable by anon: a modified client could brute-force a password and
--    call this after every failed attempt to erase its own failure count,
--    fully bypassing the 5-attempt lockout. login_lock_state() and
--    record_login_failure() were already correctly locked down; this one was
--    missed.
revoke execute on function public.clear_login_failures_by_email(text) from public, anon, authenticated;
grant execute on function public.clear_login_failures_by_email(text) to service_role;
