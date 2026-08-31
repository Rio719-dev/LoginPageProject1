package com.example.loginpageproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.loginpageproject.auth.AuthValidator
import com.example.loginpageproject.auth.LoginResult
import com.example.loginpageproject.theme.ThemeSettings
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var rememberMe: CheckBox
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        email = findViewById(R.id.etLoginEmail)
        password = findViewById(R.id.etLoginPassword)
        rememberMe = findViewById(R.id.cbRememberMe)
        loginButton = findViewById(R.id.btnLogin)

        val themeToggle = findViewById<MaterialSwitch>(R.id.swThemeToggle)
        themeToggle.isChecked = ThemeSettings.isDarkMode(this)
        themeToggle.setOnCheckedChangeListener { _, isChecked ->
            ThemeSettings.setDarkMode(this, isChecked)
            recreate()
        }

        val timedOutForInactivity = intent.getStringExtra(EXTRA_REASON) == REASON_INACTIVITY
        intent.removeExtra(EXTRA_REASON)
        if (timedOutForInactivity && savedInstanceState == null) {
            Toast.makeText(this, "You were logged out after 1 minute of inactivity.", Toast.LENGTH_LONG).show()
        }
        loginButton.setOnClickListener { login() }
        findViewById<TextView>(R.id.tvSignUpLink).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun login() {
        val enteredEmail = email.text.toString().trim()
        val enteredPassword = password.text.toString()
        if (!AuthValidator.isValidEmail(enteredEmail)) {
            email.error = "Enter a valid email address"
            return
        }
        if (enteredPassword.isBlank()) {
            password.error = "Password is required"
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            when (val result = runCatching { authRepository.login(enteredEmail, enteredPassword) }.getOrElse {
                Toast.makeText(this@LoginActivity, it.message ?: "Unable to sign in. Check your connection.", Toast.LENGTH_LONG).show()
                setLoading(false)
                return@launch
            }) {
                is LoginResult.Success -> {
                    secureSessionStore.rememberMe = rememberMe.isChecked
                    startActivity(Intent(this@LoginActivity, LandingActivity::class.java))
                    finish()
                }
                is LoginResult.Locked -> {
                    val minutes = (result.secondsRemaining + 59) / 60
                    Toast.makeText(
                        this@LoginActivity,
                        "Too many unsuccessful login attempts. Your login has been temporarily blocked. Please try again in $minutes minute(s).",
                        Toast.LENGTH_LONG
                    ).show()
                }
                LoginResult.InvalidCredentials -> {
                    Toast.makeText(this@LoginActivity, "Invalid email or password.", Toast.LENGTH_SHORT).show()
                }
            }
            setLoading(false)
        }
    }

    private fun setLoading(loading: Boolean) {
        loginButton.isEnabled = !loading
        findViewById<View>(R.id.loginProgress).visibility = if (loading) View.VISIBLE else View.GONE
    }

    companion object {
        const val EXTRA_REASON = "reason"
        const val REASON_INACTIVITY = "inactivity"
    }
}
