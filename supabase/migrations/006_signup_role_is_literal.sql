-- Makes the Access Type picked at signup literally become the account's role,
-- per the explicit written requirement: "The selected access type must be stored
-- with the account" and "Automatically determine whether the account is Super
-- Admin / Admin / User" based on what was saved at registration.
--
-- SECURITY NOTE (explicitly accepted by the project owner): this removes the
-- "only the first account can become Super Admin" bootstrap restriction added in
-- 003_role_registration_and_management.sql. From this migration onward, ANY
-- signup that selects Super Admin (or Admin) in the Access Type dropdown is
-- immediately granted that role, with no approval step. Any person who can reach
-- the public sign-up screen can grant themselves full Super Admin authority
-- (view, reset, or delete any other account). This is a deliberate, requested
-- trade-off in favor of literal spec compliance over the more conservative
-- approval-gated design.
--
-- super_admin_set_role() and super_admin_delete_user() (both from
-- 003_role_registration_and_management.sql) are UNCHANGED by this migration:
-- they still refuse to target ANY profile whose role is 'super_admin', and this
-- now matters more, not less, since multiple Super Admin accounts can exist. It
-- means a Super Admin still can never demote or delete another Super Admin
-- through this app -- only 'user'/'admin' accounts are manageable that way. That
-- residual protection is intentionally kept: it stops a single compromised or
-- malicious Super Admin session from being used to wipe out every other Super
-- Admin account.
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
      requested, -- the Access Type selected at signup is now the account's actual role
      requested
    )
    on conflict (id) do nothing;

    insert into private.password_history (user_id, password_hash)
    values (new.id, new.encrypted_password);
  end if;
  return new;
end;
$$;
