-- Fixes account role registration and adds the Super Admin management capabilities
-- that depend on it (promote/demote, delete another account).
--
-- Previously create_confirmed_profile() discarded the requested role entirely and
-- always inserted 'user', regardless of what was picked at signup. That made the
-- signup role picker misleading: a person could select "Super Admin" and still end
-- up with a plain User account forever, with no path to ever becoming Admin/Super
-- Admin short of a manual database edit.
--
-- New rules:
--   * The very first account ever confirmed (profiles table empty at insert time)
--     that requested 'super_admin' becomes the Super Admin. This is the only way a
--     Super Admin can ever be created through signup, and it can only happen once.
--   * Every other signup -- regardless of what role was requested -- is created as
--     'user'. Admin access can only be granted afterwards by an existing Super Admin
--     via super_admin_set_role() below; it is never self-assignable at signup.
--   * requested_role still records what was originally asked for, for audit
--     purposes only. It is never itself treated as an authorization decision.
create or replace function public.create_confirmed_profile()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  requested text := coalesce(new.raw_user_meta_data ->> 'requested_role', 'user');
  effective_role text := 'user';
begin
  if old.email_confirmed_at is null and new.email_confirmed_at is not null then
    if requested not in ('super_admin', 'admin', 'user') then requested := 'user'; end if;

    -- Bootstrap rule: only the first confirmed account in the whole system can become
    -- Super Admin, and only if it explicitly requested that role.
    if requested = 'super_admin' and not exists (select 1 from public.profiles) then
      effective_role := 'super_admin';
    end if;

    insert into public.profiles (id, email, full_name, birthday, address, mobile, role, requested_role)
    values (
      new.id,
      new.email,
      coalesce(new.raw_user_meta_data ->> 'full_name', ''),
      (new.raw_user_meta_data ->> 'birthday')::date,
      coalesce(new.raw_user_meta_data ->> 'address', ''),
      coalesce(new.raw_user_meta_data ->> 'mobile', ''),
      effective_role,
      requested
    )
    on conflict (id) do nothing;

    insert into private.password_history (user_id, password_hash)
    values (new.id, new.encrypted_password);
  end if;
  return new;
end;
$$;

-- Lets an existing Super Admin promote a User to Admin, or demote an Admin back to
-- User. Deliberately cannot target a Super Admin account or assign the 'super_admin'
-- role to anyone -- the sole Super Admin can only be created by the bootstrap rule
-- above (or a direct database edit), so this function can never mint another one
-- and can never be used to demote the existing one.
create or replace function public.super_admin_set_role(p_user_id uuid, p_new_role text)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  target_role text;
begin
  if not public.is_super_admin() then raise exception 'Super Admin access required'; end if;
  if p_new_role not in ('user', 'admin') then raise exception 'Invalid role'; end if;
  if p_user_id = auth.uid() then raise exception 'Cannot change your own role'; end if;

  select role into target_role from public.profiles where id = p_user_id;
  if target_role is null then raise exception 'User not found'; end if;
  if target_role = 'super_admin' then raise exception 'Cannot change the Super Admin role'; end if;

  update public.profiles set role = p_new_role, updated_at = now() where id = p_user_id;
  return true;
end;
$$;

grant execute on function public.super_admin_set_role(uuid, text) to authenticated;

-- Lets an existing Super Admin permanently delete another account. Deletes the
-- auth.users row directly (the same privileged pattern already used by
-- change_my_password() to write auth.users from a security-definer function).
-- public.profiles and private.password_history both declare
-- "references auth.users(id) on delete cascade", so this single delete also
-- removes the profile and password history; Supabase's own auth.sessions /
-- auth.refresh_tokens tables cascade from auth.users the same way, so the
-- deleted account's active sessions are invalidated too.
--
-- Cannot delete your own account this way (use a dedicated self-deletion flow if
-- ever needed) and can never delete a Super Admin -- there is exactly one Super
-- Admin account (see create_confirmed_profile()) and it is never a valid delete
-- target here.
create or replace function public.super_admin_delete_user(p_user_id uuid)
returns boolean
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  target_role text;
begin
  if not public.is_super_admin() then raise exception 'Super Admin access required'; end if;
  if p_user_id = auth.uid() then raise exception 'Cannot delete your own account'; end if;

  select role into target_role from public.profiles where id = p_user_id;
  if target_role is null then raise exception 'User not found'; end if;
  if target_role = 'super_admin' then raise exception 'Cannot delete the Super Admin account'; end if;

  delete from auth.users where id = p_user_id;
  return true;
end;
$$;

grant execute on function public.super_admin_delete_user(uuid) to authenticated;
