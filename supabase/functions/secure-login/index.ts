// Supabase Edge Function: secure-login
//
// This function is the ONLY place login attempts are counted. The Android app
// never calls Supabase Auth's sign-in endpoint directly and never decides for
// itself whether a failure gets recorded — a modified or rooted client cannot
// skip the lockout counter because it never has access to the increment step.
//
// Deploy with: supabase functions deploy secure-login --no-verify-jwt
// (verify_jwt is disabled because this endpoint authenticates the password
// itself; it is the credential check, not a caller that already holds a JWT.)
//
// Required secrets (set via `supabase secrets set` or the dashboard):
//   SUPABASE_URL              - auto-provisioned by the platform
//   SUPABASE_SERVICE_ROLE_KEY - auto-provisioned by the platform (never sent to the client)

import { createClient } from "jsr:@supabase/supabase-js@2";

// The real threshold lives only in record_login_failure() (SQL, migration 005);
// this constant is documentation only and is not itself the enforcement point.
const MAX_ATTEMPTS = 3;
const LOCKOUT_MINUTES = 5;

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

// Privileged client: only reachable from inside this server-side function.
const adminClient = createClient(supabaseUrl, serviceRoleKey);

// The lockout table lives in the "private" schema, which is intentionally
// NOT exposed through the Data API (only "public" is exposed by default).
// Querying it directly via .schema("private").from(...) is rejected by the
// API gateway. Instead we call the public, security-definer SQL functions
// from the migration (login_lock_state / record_login_failure /
// clear_login_failures_by_email) via RPC — they run as the function owner
// and reach into "private" from inside Postgres, so the schema itself never
// needs to be exposed to any API caller, not even the service role.

function jsonResponse(body: Record<string, unknown>, status: number) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  let email: string;
  let password: string;
  try {
    const body = await req.json();
    email = String(body.email ?? "").trim().toLowerCase();
    password = String(body.password ?? "");
  } catch {
    return jsonResponse({ error: "Invalid request body" }, 400);
  }

  if (!email || !password) {
    return jsonResponse({ error: "Email and password are required" }, 400);
  }

  // 1. Check lockout state first via the security-definer SQL function.
  // login_lock_state() returns a single row: { is_locked, seconds_remaining }.
  const { data: lockStateRows, error: lockStateError } = await adminClient
    .rpc("login_lock_state", { p_email: email });

  if (lockStateError) {
    console.error("login_lock_state RPC failed", lockStateError);
    return jsonResponse({ error: "Unable to verify account status" }, 500);
  }

  const lockState = Array.isArray(lockStateRows) ? lockStateRows[0] : lockStateRows;
  if (lockState?.is_locked) {
    return jsonResponse({ locked: true, secondsRemaining: Number(lockState.seconds_remaining ?? 0) }, 423);
  }

  // 2. Attempt the actual password check using a short-lived anon-equivalent
  // sign-in call. This is the credential verification step; the client never
  // performs this itself.
  const { data: signInData, error: signInError } = await adminClient.auth.signInWithPassword({
    email,
    password,
  });

  if (signInError || !signInData.session) {
    // 3. Record the failure via the security-definer function. This function
    // owns the entire increment/lockout decision inside Postgres — there is
    // no client code path (or even a code path in this function) that can
    // skip or tamper with the increment; it always runs on every failure.
    const { error: recordError } = await adminClient
      .rpc("record_login_failure", { p_email: email });

    if (recordError) {
      console.error("record_login_failure RPC failed", recordError);
      return jsonResponse({ error: "Invalid email or password" }, 401);
    }

    // Re-check lock state to see whether this failure just triggered the lock.
    const { data: postFailureRows } = await adminClient
      .rpc("login_lock_state", { p_email: email });
    const postFailureState = Array.isArray(postFailureRows) ? postFailureRows[0] : postFailureRows;

    if (postFailureState?.is_locked) {
      return jsonResponse(
        { locked: true, secondsRemaining: Number(postFailureState.seconds_remaining ?? LOCKOUT_MINUTES * 60) },
        423
      );
    }
    return jsonResponse({ error: "Invalid email or password" }, 401);
  }

  // 4. Success: clear the failure counter via the security-definer function,
  // then hand back the session for the client to import. The client receives
  // tokens, never credentials logic.
  const { error: clearError } = await adminClient
    .rpc("clear_login_failures_by_email", { p_email: email });
  if (clearError) {
    console.error("clear_login_failures_by_email RPC failed", clearError);
  }

  return jsonResponse({
    accessToken: signInData.session.access_token,
    refreshToken: signInData.session.refresh_token,
    expiresIn: signInData.session.expires_in,
    tokenType: signInData.session.token_type,
    userId: signInData.user?.id,
  }, 200);
});
