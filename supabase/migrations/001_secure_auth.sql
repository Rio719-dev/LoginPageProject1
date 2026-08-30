-- Apply this migration in the Supabase SQL editor before configuring the Android client.
-- Never place SUPABASE_SERVICE_ROLE_KEY in the Android application.
create extension if not exists pgcrypto;
create schema if not exists private;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null unique,
  full_name text not null,
  birthday date not null,
  address text not null,
  mobile text not null,
  role text not null default 'user' check (role in ('super_admin', 'admin', 'user')),
  requested_role text not null default 'user' check (requested_role in ('super_admin', 'admin', 'user')),
  must_change_password boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists private.password_history (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  password_hash text not null,
  created_at timestamptz not null default now()
);

create table if not exists private.login_failures (
  email text primary key,
  failed_attempts integer not null default 0 check (failed_attempts >= 0),
  locked_until timestamptz,
  updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table private.password_history enable row level security;
alter table private.login_failures enable row level security;

create or replace function public.is_super_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.profiles
    where id = auth.uid() and role = 'super_admin'
  );
$$;

-- Admins may read (monitor) every profile but cannot modify roles or trigger password
-- resets. Only Super Admin gets that power, via super_admin_initiate_password_reset()
-- and the update policy below.
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

drop policy if exists "users update own profile or super admins update all" on public.profiles;
create policy "users update own profile or super admins update all"
on public.profiles for update to authenticated
using (id = auth.uid() or public.is_super_admin())
with check (id = auth.uid() or public.is_super_admin());

-- Prevent ordinary clients from changing a role even though profile updates are otherwise allowed.
revoke update (role, requested_role, must_change_password, email) on public.profiles from authenticated;
grant update (full_name, birthday, address, mobile) on public.profiles to authenticated;

create or replace function public.create_confirmed_profile()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  requested text := coalesce(new.raw_user_meta_data ->> 'requested_role', 'user');
begin
  if old.email_confirmed_at is null and new.email_confirmed_at is not null then
    if requested not in ('super_admin', 'admin', 'user') then requested := 'user'; end if;
    insert into public.profiles (id, email, full_name, birthday, address, mobile, role, requested_role)
    values (
      new.id,
      new.email,
      coalesce(new.raw_user_meta_data ->> 'full_name', ''),
      (new.raw_user_meta_data ->> 'birthday')::date,
      coalesce(new.raw_user_meta_data ->> 'address', ''),
      coalesce(new.raw_user_meta_data ->> 'mobile', ''),
      'user', -- Public signup can never self-assign a privileged trusted role.
      requested
    )
    on conflict (id) do nothing;

    insert into private.password_history (user_id, password_hash)
    values (new.id, new.encrypted_password);
  end if;
  return new;
end;
$$;

drop trigger if exists create_profile_on_email_confirmation on auth.users;
create trigger create_profile_on_email_confirmation
after update of email_confirmed_at on auth.users
for each row execute procedure public.create_confirmed_profile();

create or replace function public.email_is_registered(p_email text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists(select 1 from public.profiles where email = lower(trim(p_email)));
$$;

create or replace function public.login_lock_state(p_email text)
returns table(is_locked boolean, seconds_remaining bigint)
language sql
stable
security definer
set search_path = private, public
as $$
  select
    coalesce(locked_until > now(), false),
    greatest(0, coalesce(extract(epoch from (locked_until - now()))::bigint, 0))
  from private.login_failures
  where email = lower(trim(p_email))
  union all select false, 0
  where not exists(select 1 from private.login_failures where email = lower(trim(p_email)))
  limit 1;
$$;

-- This function is called only after a failed GoTrue sign-in. For tamper-resistant production
-- enforcement, invoke it from a Supabase Edge Function that performs password authentication,
-- not directly from an untrusted client.
create or replace function public.record_login_failure(p_email text)
returns boolean
language plpgsql
security definer
set search_path = private, public
as $$
declare
  normalized_email text := lower(trim(p_email));
begin
  insert into private.login_failures (email, failed_attempts, locked_until)
  values (normalized_email, 1, null)
  on conflict (email) do update
  set failed_attempts = case
      when private.login_failures.locked_until is not null and private.login_failures.locked_until <= now() then 1
      else private.login_failures.failed_attempts + 1
    end,
    locked_until = case
      when (case when private.login_failures.locked_until is not null and private.login_failures.locked_until <= now()
            then 1 else private.login_failures.failed_attempts + 1 end) >= 5
      then now() + interval '5 minutes'
      else null
    end,
    updated_at = now();
  return true;
end;
$$;

create or replace function public.clear_my_login_failures()
returns boolean
language plpgsql
security definer
set search_path = private, auth
as $$
begin
  delete from private.login_failures where email = lower((select email from auth.users where id = auth.uid()));
  return true;
end;
$$;

-- Called only by the secure-login Edge Function (service_role) after it has
-- verified the password itself. Never grant this to anon/authenticated: a
-- client that could clear its own failure count could also skip recording one.
create or replace function public.clear_login_failures_by_email(p_email text)
returns boolean
language plpgsql
security definer
set search_path = private
as $$
begin
  delete from private.login_failures where email = lower(trim(p_email));
  return true;
end;
$$;

create or replace function public.change_my_password(p_new_password text)
returns boolean
language plpgsql
security definer
set search_path = public, private, auth, extensions
as $$
declare
  current_user_id uuid := auth.uid();
  new_hash text;
begin
  if current_user_id is null then raise exception 'Authentication required'; end if;
  if length(p_new_password) not between 8 and 16
     or p_new_password !~ '[A-Z]' or p_new_password !~ '[a-z]'
     or p_new_password !~ '[0-9]' or p_new_password !~ '[^A-Za-z0-9]' then
    raise exception 'Password does not meet requirements';
  end if;
  if exists (
    select 1 from private.password_history
    where user_id = current_user_id and crypt(p_new_password, password_hash) = password_hash
  ) then
    raise exception 'You cannot reuse a previous password. Please choose a new password.';
  end if;
  new_hash := crypt(p_new_password, gen_salt('bf'));
  update auth.users set encrypted_password = new_hash, updated_at = now() where id = current_user_id;
  insert into private.password_history (user_id, password_hash) values (current_user_id, new_hash);
  update public.profiles set must_change_password = false, updated_at = now() where id = current_user_id;
  return true;
end;
$$;

create or replace function public.super_admin_initiate_password_reset(p_user_id uuid)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_super_admin() then raise exception 'Super Admin access required'; end if;
  update public.profiles set must_change_password = true, updated_at = now() where id = p_user_id;
  if not found then raise exception 'User not found'; end if;
  return true;
end;
$$;

grant execute on function public.is_super_admin() to authenticated;
grant execute on function public.is_admin_or_super_admin() to authenticated;
grant execute on function public.email_is_registered(text) to anon, authenticated;
grant execute on function public.clear_my_login_failures() to authenticated;
grant execute on function public.change_my_password(text) to authenticated;
grant execute on function public.super_admin_initiate_password_reset(uuid) to authenticated;

-- login_lock_state / record_login_failure / clear_login_failures_by_email are
-- deliberately NOT granted to anon or authenticated. Only the secure-login
-- Edge Function (running with the service_role key) may read or mutate lockout
-- state. This is what prevents a modified client from bypassing the 5-attempt
-- lockout: the client has no code path capable of calling these directly, and
-- login itself is now only reachable through the Edge Function.
revoke execute on function public.login_lock_state(text) from public, anon, authenticated;
revoke execute on function public.record_login_failure(text) from public, anon, authenticated;
grant execute on function public.login_lock_state(text) to service_role;
grant execute on function public.record_login_failure(text) to service_role;
grant execute on function public.clear_login_failures_by_email(text) to service_role;

grant select, insert, update on public.profiles to authenticated;
