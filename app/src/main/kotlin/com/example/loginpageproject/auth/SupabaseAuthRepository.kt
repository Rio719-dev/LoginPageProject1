package com.example.loginpageproject.auth

import com.example.loginpageproject.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.Serializable

/**
 * The sole mobile authentication gateway. Passwords are never stored locally.
 *
 * Login does NOT call Supabase Auth's sign-in endpoint directly. Instead it calls the
 * `secure-login` Edge Function, which performs the password check itself using the
 * service-role key and owns the entire failed-attempt counter and lockout decision
 * server-side. The Android client has no code path that can record a login attempt
 * or skip the lockout check — a modified/rooted client cannot bypass the 3-attempt,
 * 5-minute lockout because the client never controls whether a failure is recorded.
 */
object SupabaseProvider {
    val client: SupabaseClient by lazy {
        val url = BuildConfig.SUPABASE_URL.trim()
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (url.isBlank() || key.isBlank()) {
            throw AuthConfigurationException("Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to local gradle.properties.")
        }
        createSupabaseClient(url, key) {
            install(Auth)
            install(Postgrest)
            install(Functions)
        }
    }
}

@Serializable
private data class SecureLoginRequest(val email: String, val password: String)

@Serializable
private data class SecureLoginSuccess(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String,
    val userId: String? = null
)

@Serializable
private data class SecureLoginLockedBody(val locked: Boolean = false, val secondsRemaining: Long = 0)

interface AuthRepository {
    suspend fun emailExists(email: String): Boolean
    suspend fun register(data: RegistrationData)
    suspend fun verifySignupOtp(email: String, code: String)
    suspend fun resendSignupOtp(email: String)
    suspend fun login(email: String, password: String): LoginResult
    suspend fun sendRecoveryOtp(email: String)
    suspend fun verifyRecoveryOtp(email: String, code: String)
    suspend fun changePassword(newPassword: String)
    /** True if there is any active Supabase session (normal login OR a short-lived recovery-OTP session). */
    suspend fun currentSessionExists(): Boolean
    suspend fun currentProfile(): UserProfile?
    suspend fun searchUsers(emailQuery: String): List<UserProfile>
    /** Read-only directory for Admin/Super Admin monitoring. Server RLS enforces who may call this. */
    suspend fun listAllUsers(): List<UserProfile>
    suspend fun initiateAdminReset(userId: String, email: String)
    /** Super Admin only: promotes a User to Admin, or demotes an Admin back to User. */
    suspend fun setUserRole(userId: String, newRole: AccessRole)
    /** Super Admin only: permanently deletes another account. Cannot target self or the Super Admin. */
    suspend fun deleteUser(userId: String)
    suspend fun signOut()
}

class SupabaseAuthRepository(
    private val client: SupabaseClient = SupabaseProvider.client
) : AuthRepository {
    override suspend fun emailExists(email: String): Boolean {
        // email_is_registered() is `returns boolean` — a scalar, so PostgREST responds
        // with a bare `false`/`true`, not a JSON array. decodeSingle() always expects an
        // array (it's decodeList().first() under the hood), so it must NOT be used here;
        // decodeAs() decodes the raw scalar body directly.
        return client.postgrest.rpc(
            "email_is_registered",
            buildJsonObject { put("p_email", email.lowercase()) }
        ).decodeAs<Boolean>()
    }

    override suspend fun register(data: RegistrationData) {
        client.auth.signUpWith(Email) {
            email = data.email.lowercase()
            password = data.password
            this.data = buildJsonObject {
                put("full_name", data.fullName)
                put("birthday", data.birthday)
                put("address", data.address)
                put("mobile", data.mobile)
                // The signup trigger (create_confirmed_profile, migration 006) copies this
                // value directly into profiles.role -- the picked Access Type IS the
                // account's actual role, with no approval step. See migration 006 for the
                // accepted security trade-off.
                put("requested_role", data.requestedRole)
            }
        }
    }

    override suspend fun verifySignupOtp(email: String, code: String) {
        client.auth.verifyEmailOtp(OtpType.Email.SIGNUP, email.lowercase(), code)
        // Email verification can establish a temporary session; requirements demand manual login.
        client.auth.signOut()
    }

    override suspend fun resendSignupOtp(email: String) {
        client.auth.resendEmail(OtpType.Email.SIGNUP, email.lowercase())
    }

    override suspend fun login(email: String, password: String): LoginResult {
        val normalizedEmail = email.lowercase()

        // The password check and the entire failed-attempt counter live inside the
        // secure-login Edge Function, which runs with the service_role key. This
        // repository never calls Supabase Auth's sign-in endpoint and never calls
        // any lockout-related database function directly — those are revoked from
        // anon/authenticated in the migration. A modified client cannot bypass the
        // 3-attempt/5-minute lockout because it has no path to the logic that
        // decides whether an attempt counts as a failure.
        val response: HttpResponse = client.functions.invoke(
            function = "secure-login",
            body = SecureLoginRequest(normalizedEmail, password)
        )

        return when (response.status) {
            HttpStatusCode.OK -> {
                val success = response.body<SecureLoginSuccess>()
                client.auth.importSession(
                    UserSession(
                        accessToken = success.accessToken,
                        refreshToken = success.refreshToken,
                        expiresIn = success.expiresIn,
                        tokenType = success.tokenType
                    ),
                    source = SessionSource.SignIn(Email)
                )
                // importSession alone does not populate the user; fetch it explicitly
                // and update the session status so currentUserOrNull()/currentProfile() work.
                client.auth.retrieveUserForCurrentSession(updateSession = true)
                val profile = requireNotNull(currentProfile()) { "Account profile is unavailable. Contact support." }
                LoginResult.Success(profile)
            }
            HttpStatusCode.Locked -> {
                val locked = response.body<SecureLoginLockedBody>()
                LoginResult.Locked(locked.secondsRemaining)
            }
            else -> LoginResult.InvalidCredentials
        }
    }

    override suspend fun sendRecoveryOtp(email: String) {
        client.auth.resetPasswordForEmail(email.lowercase())
    }

    override suspend fun verifyRecoveryOtp(email: String, code: String) {
        client.auth.verifyEmailOtp(OtpType.Email.RECOVERY, email.lowercase(), code)
    }

    override suspend fun changePassword(newPassword: String) {
        try {
            // change_my_password() is `returns boolean` — a scalar body, not an array.
            client.postgrest.rpc(
                "change_my_password",
                buildJsonObject { put("p_new_password", newPassword) }
            ).decodeAs<Boolean>()
        } catch (error: Exception) {
            if (error.message.orEmpty().contains("previous password", ignoreCase = true)) {
                throw PasswordReuseException()
            }
            throw error
        }
    }

    override suspend fun currentSessionExists(): Boolean = client.auth.currentUserOrNull() != null

    override suspend fun currentProfile(): UserProfile? {
        val userId = client.auth.currentUserOrNull()?.id ?: return null
        return runCatching {
            client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingle<UserProfile>()
        }.getOrNull()
    }

    override suspend fun searchUsers(emailQuery: String): List<UserProfile> {
        val escapedQuery = emailQuery.trim().replace("%", "\\%").replace("_", "\\_")
        return client.from("profiles")
            .select { filter { ilike("email", "%$escapedQuery%") } }
            .decodeList()
    }

    override suspend fun listAllUsers(): List<UserProfile> {
        // RLS on profiles allows select only for: the row owner, an admin, or a super admin
        // (see is_admin_or_super_admin() in the migration). A plain User calling this will
        // simply receive just their own row back, never other users' data.
        return client.from("profiles")
            .select { order("full_name", Order.ASCENDING) }
            .decodeList()
    }

    override suspend fun initiateAdminReset(userId: String, email: String) {
        // super_admin_initiate_password_reset() is `returns boolean` — a scalar body.
        client.postgrest.rpc(
            "super_admin_initiate_password_reset",
            buildJsonObject { put("p_user_id", userId) }
        ).decodeAs<Boolean>()
        client.auth.resetPasswordForEmail(email)
    }

    override suspend fun setUserRole(userId: String, newRole: AccessRole) {
        require(newRole == AccessRole.USER || newRole == AccessRole.ADMIN) {
            "Only User or Admin can be assigned; Super Admin cannot be granted."
        }
        // super_admin_set_role() is `returns boolean` — a scalar body.
        client.postgrest.rpc(
            "super_admin_set_role",
            buildJsonObject {
                put("p_user_id", userId)
                put("p_new_role", newRole.databaseValue)
            }
        ).decodeAs<Boolean>()
    }

    override suspend fun deleteUser(userId: String) {
        // super_admin_delete_user() is `returns boolean` — a scalar body.
        client.postgrest.rpc(
            "super_admin_delete_user",
            buildJsonObject { put("p_user_id", userId) }
        ).decodeAs<Boolean>()
    }

    override suspend fun signOut() {
        runCatching { client.auth.signOut() }
    }
}
