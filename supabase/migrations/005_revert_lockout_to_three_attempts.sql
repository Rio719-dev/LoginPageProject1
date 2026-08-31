-- Reverts the login lockout threshold from five failed attempts back to three,
-- to match the written "final requirements" spec (3 incorrect attempts, then a
-- 5-minute lockout). Migration 002 had deliberately raised this from 3 to 5;
-- this migration deliberately reverts that decision now that the spec is explicit.
-- The 5-minute lockout duration itself is unchanged and already matched the spec.
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
            then 1 else private.login_failures.failed_attempts + 1 end) >= 3
      then now() + interval '5 minutes'
      else null
    end,
    updated_at = now();
  return true;
end;
$$;
