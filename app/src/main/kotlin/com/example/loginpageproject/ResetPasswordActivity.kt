package com.example.loginpageproject

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.loginpageproject.auth.AuthValidator
import com.example.loginpageproject.auth.PasswordReuseException
import kotlinx.coroutines.launch

class ResetPasswordActivity : BaseActivity() {
    // MODE_AUTHENTICATED requires an existing signed-in session (reached from Landing),
    // so the shared BaseActivity session check/inactivity timer applies to that path.
    // MODE_RECOVERY relies on the short-lived session verifyRecoveryOtp() establishes
    // right before navigating here, which is intentionally NOT the app's normal
    // "signed in" session (no inactivity timer/redirect makes sense mid-recovery), so
    // it is checked independently in onCreate() below instead of via requiresAuthentication.
    override val requiresAuthentication: Boolean
        get() = mode == MODE_AUTHENTICATED

    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var saveButton: Button
    private lateinit var strengthBar: ProgressBar
    private lateinit var mode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        mode = intent.getStringExtra(EXTRA_MODE).takeIf { it in setOf(MODE_RECOVERY, MODE_AUTHENTICATED) } ?: MODE_AUTHENTICATED
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)
        password = findViewById(R.id.etNewPassword)
        confirmPassword = findViewById(R.id.etConfirmPassword)
        saveButton = findViewById(R.id.btnReset)
        strengthBar = findViewById(R.id.pbPasswordStrength)

        if (mode == MODE_RECOVERY) {
            lifecycleScope.launch {
                // A recovery OTP verification must have just run and produced a session;
                // if there isn't one (e.g. this screen was reached directly, or the
                // temporary recovery session already expired), there is nothing valid to
                // act on, so send the user back to start the recovery flow again instead
                // of letting them hit the "Authentication required" error from the SQL
                // function after typing a full new password.
                if (authRepository.currentSessionExists().not()) {
                    Toast.makeText(this@ResetPasswordActivity, "Your verification session expired. Please request a new code.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@ResetPasswordActivity, ForgotPasswordActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                }
            }
        }
        password.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = displayRules(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
        saveButton.setOnClickListener { changePassword() }
    }

    private fun displayRules(value: String) {
        val rules = AuthValidator.passwordRequirements(value)
        findViewById<TextView>(R.id.tvStrengthIndicator).text = when {
            rules.isStrong -> "🟢 Strong — all password requirements are met"
            rules.score >= 3 -> "🟡 Average — complete all requirements"
            else -> "🔴 Weak — complete all requirements"
        }
        strengthBar.updatePasswordStrengthBar(rules)
    }

    private fun changePassword() {
        val newPassword = password.text.toString()
        val confirmation = confirmPassword.text.toString()
        if (!AuthValidator.isStrongPassword(newPassword)) {
            password.error = "Password must be 8–16 characters with uppercase, lowercase, number, and special character"
            return
        }
        if (newPassword != confirmation) {
            confirmPassword.error = "Passwords do not match"
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            runCatching { authRepository.changePassword(newPassword) }
                .onSuccess {
                    authRepository.signOut()
                    secureSessionStore.clear()
                    Toast.makeText(this@ResetPasswordActivity, "Password updated. Please log in again.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@ResetPasswordActivity, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                }
                .onFailure {
                    val message = if (it is PasswordReuseException) it.message else it.message ?: "Unable to update password."
                    Toast.makeText(this@ResetPasswordActivity, message, Toast.LENGTH_LONG).show()
                }
            setLoading(false)
        }
    }

    private fun setLoading(loading: Boolean) {
        saveButton.isEnabled = !loading
        findViewById<View>(R.id.resetProgress).visibility = if (loading) View.VISIBLE else View.GONE
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_RECOVERY = "recovery"
        const val MODE_AUTHENTICATED = "authenticated"
    }
}
