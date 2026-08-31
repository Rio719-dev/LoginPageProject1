package com.example.loginpageproject

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.loginpageproject.auth.SecureSessionStore
import com.example.loginpageproject.auth.SupabaseAuthRepository
import com.example.loginpageproject.auth.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {
    protected open val requiresAuthentication = false
    protected val authRepository by lazy { SupabaseAuthRepository() }
    protected val secureSessionStore by lazy { SecureSessionStore(this) }

    private val handler = Handler(Looper.getMainLooper())
    private var expiryDialog: AlertDialog? = null
    private var sessionCheckJob: Job? = null
    private var isLoggingOut = false
    private var isRedirectingToLogin = false
    private val warningRunnable = Runnable { showExpiryWarning() }
    private val logoutRunnable = Runnable { performIdleLogout() }

    override fun onResume() {
        super.onResume()
        if (!requiresAuthentication) return

        sessionCheckJob?.cancel()
        sessionCheckJob = lifecycleScope.launch {
            val profile = authRepository.currentProfile()
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@launch

            if (profile == null) {
                redirectToLogin(null)
            } else {
                resetInactivityTimer()
                onAuthenticatedProfileAvailable(profile)
            }
        }
    }

    override fun onPause() {
        sessionCheckJob?.cancel()
        sessionCheckJob = null
        stopInactivityTimer()
        super.onPause()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (requiresAuthentication && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            resetInactivityTimer()
        }
    }

    /** Called after the shared authenticated-session check succeeds on resume. */
    protected open fun onAuthenticatedProfileAvailable(profile: UserProfile) = Unit

    protected fun resetInactivityTimer() {
        if (
            isLoggingOut ||
            isRedirectingToLogin ||
            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) return

        stopInactivityTimer()
        handler.postDelayed(warningRunnable, WARNING_DELAY_MS)
        handler.postDelayed(logoutRunnable, TIMEOUT_MS)
    }

    /** Stops this activity's pending inactivity warning and logout callbacks. */
    protected fun cancelInactivityTimer() {
        stopInactivityTimer()
    }

    private fun stopInactivityTimer() {
        handler.removeCallbacks(warningRunnable)
        handler.removeCallbacks(logoutRunnable)
        expiryDialog?.dismiss()
        expiryDialog = null
    }

    private fun showExpiryWarning() {
        if (isFinishing || isDestroyed || isLoggingOut || isRedirectingToLogin || expiryDialog?.isShowing == true) return
        expiryDialog = AlertDialog.Builder(this)
            .setTitle("Session expiring")
            .setMessage("Your session will expire soon due to inactivity.")
            .setPositiveButton("Stay signed in") { _, _ -> resetInactivityTimer() }
            .setNegativeButton("Log out") { _, _ -> performIdleLogout() }
            .setCancelable(false)
            .show()
    }

    private fun performIdleLogout() {
        if (isLoggingOut || isRedirectingToLogin || isFinishing || isDestroyed) return
        isLoggingOut = true
        stopInactivityTimer()
        lifecycleScope.launch {
            authRepository.signOut()
            secureSessionStore.clear()
            redirectToLogin(LoginActivity.REASON_INACTIVITY)
        }
    }

    protected fun redirectToLogin(reason: String?) {
        if (isRedirectingToLogin || isFinishing || isDestroyed) return
        isRedirectingToLogin = true
        stopInactivityTimer()
        val intent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        reason?.let { intent.putExtra(LoginActivity.EXTRA_REASON, it) }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        stopInactivityTimer()
        super.onDestroy()
    }

    private companion object {
        const val TIMEOUT_MS = 60_000L
        const val WARNING_DELAY_MS = 50_000L
    }
}
