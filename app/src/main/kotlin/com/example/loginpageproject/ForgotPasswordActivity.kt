package com.example.loginpageproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.loginpageproject.auth.AuthValidator
import kotlinx.coroutines.launch

class ForgotPasswordActivity : BaseActivity() {
    private lateinit var email: EditText
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        email = findViewById(R.id.etEmail)
        sendButton = findViewById(R.id.btnSendCode)
        sendButton.setOnClickListener { sendCode() }
        findViewById<TextView>(R.id.tvBackToLogin).setOnClickListener { finish() }
    }

    private fun sendCode() {
        val enteredEmail = email.text.toString().trim()
        if (!AuthValidator.isValidEmail(enteredEmail)) {
            email.error = "Enter a valid registered email address"
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            // Explicit existence check per spec: the account must be validated before
            // sending an OTP. Note this intentionally reveals whether an email is
            // registered (an account-enumeration trade-off accepted in favor of literal
            // spec compliance over the more conservative generic-message pattern).
            val exists = runCatching { authRepository.emailExists(enteredEmail) }.getOrElse {
                Toast.makeText(this@ForgotPasswordActivity, it.message ?: "Unable to validate email.", Toast.LENGTH_LONG).show()
                setLoading(false)
                return@launch
            }
            if (!exists) {
                email.error = "No account is registered with this email address."
                setLoading(false)
                return@launch
            }
            runCatching { authRepository.sendRecoveryOtp(enteredEmail) }
                .onSuccess {
                    Toast.makeText(this@ForgotPasswordActivity, "A reset code was sent to your email.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@ForgotPasswordActivity, OTPActivity::class.java).apply {
                        putExtra(OTPActivity.EXTRA_EMAIL, enteredEmail)
                        putExtra(OTPActivity.EXTRA_MODE, OTPActivity.MODE_RECOVERY)
                    })
                }
                .onFailure { Toast.makeText(this@ForgotPasswordActivity, it.message ?: "Unable to send reset code.", Toast.LENGTH_LONG).show() }
            setLoading(false)
        }
    }

    private fun setLoading(loading: Boolean) {
        sendButton.isEnabled = !loading
        findViewById<View>(R.id.forgotProgress).visibility = if (loading) View.VISIBLE else View.GONE
    }
}
