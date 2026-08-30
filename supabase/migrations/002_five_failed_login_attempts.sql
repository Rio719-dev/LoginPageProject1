-- Raise the account lockout threshold from three to five failed passwords.
-- Existing deployments need this migration because 001_secure_auth.sql has
-- already been applied to their database.
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
