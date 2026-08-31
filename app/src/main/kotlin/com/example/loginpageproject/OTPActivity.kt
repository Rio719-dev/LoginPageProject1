package com.example.loginpageproject

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class OTPActivity : BaseActivity() {
    private lateinit var email: String
    private lateinit var mode: String
    private lateinit var digitBoxes: List<EditText>
    private lateinit var timerText: TextView
    private lateinit var verifyButton: Button
    private lateinit var resendButton: Button
    private var attempts = 0
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)
        email = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
        mode = intent.getStringExtra(EXTRA_MODE).orEmpty()
        if (email.isBlank() || mode !in setOf(MODE_SIGN_UP, MODE_RECOVERY)) {
            finish()
            return
        }
        digitBoxes = listOf(
            findViewById(R.id.otpDigit1), findViewById(R.id.otpDigit2), findViewById(R.id.otpDigit3),
            findViewById(R.id.otpDigit4), findViewById(R.id.otpDigit5), findViewById(R.id.otpDigit6)
        )
        timerText = findViewById(R.id.tvOtpCountdown)
        verifyButton = findViewById(R.id.btnVerifyOtp)
        resendButton = findViewById(R.id.btnResendOtp)
        findViewById<TextView>(R.id.tvOtpInstruction).text = "Enter the 6-digit code sent to $email"

        setupDigitBoxAutoAdvance()
        verifyButton.setOnClickListener { verify() }
        resendButton.setOnClickListener { resend() }
        startTimer()
        digitBoxes.first().requestFocus()
    }

    /** Wires each single-character box to auto-advance forward on entry and back on delete. */
    private fun setupDigitBoxAutoAdvance() {
        digitBoxes.forEachIndexed { index, box ->
            box.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && index < digitBoxes.lastIndex) {
                        digitBoxes[index + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
            box.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN &&
                    box.text.isEmpty() && index > 0
                ) {
                    digitBoxes[index - 1].apply {
                        requestFocus()
                        text.clear()
                    }
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun enteredCode(): String = digitBoxes.joinToString("") { it.text.toString() }

    private fun clearDigitBoxes() {
        digitBoxes.forEach { it.text.clear() }
        digitBoxes.first().requestFocus()
    }

    private fun startTimer() {
        timer?.cancel()
        resendButton.isEnabled = false
        timer = object : CountDownTimer(OTP_WINDOW_MS, 1_000) {
            override fun onTick(remaining: Long) {
                timerText.text = "Code expires in %02d:%02d".format(remaining / 60_000, (remaining / 1_000) % 60)
            }
            override fun onFinish() {
                timerText.text = "Code expired. Request a new code."
                resendButton.isEnabled = true
            }
        }.start()
    }

    private fun verify() {
        val code = enteredCode()
        if (code.length != 6) {
            Toast.makeText(this, "Enter the full 6-digit code", Toast.LENGTH_SHORT).show()
            return
        }
        if (++attempts > MAX_OTP_ATTEMPTS) {
            Toast.makeText(this, "Too many invalid code attempts. Request a new code.", Toast.LENGTH_LONG).show()
            verifyButton.isEnabled = false
            resendButton.isEnabled = true
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            val result = runCatching {
                if (mode == MODE_SIGN_UP) authRepository.verifySignupOtp(email, code)
                else authRepository.verifyRecoveryOtp(email, code)
            }
            result.onSuccess {
                if (mode == MODE_SIGN_UP) {
                    startActivity(Intent(this@OTPActivity, SuccessActivity::class.java))
                } else {
                    startActivity(Intent(this@OTPActivity, ResetPasswordActivity::class.java).putExtra(ResetPasswordActivity.EXTRA_MODE, ResetPasswordActivity.MODE_RECOVERY))
                }
                finish()
            }.onFailure {
                Toast.makeText(this@OTPActivity, it.message ?: "Invalid or expired verification code.", Toast.LENGTH_LONG).show()
            }
            setLoading(false)
        }
    }

    private fun resend() {
        setLoading(true)
        lifecycleScope.launch {
            val result = runCatching {
                if (mode == MODE_SIGN_UP) authRepository.resendSignupOtp(email) else authRepository.sendRecoveryOtp(email)
            }
            result.onSuccess {
                attempts = 0
                verifyButton.isEnabled = true
                clearDigitBoxes()
                startTimer()
                Toast.makeText(this@OTPActivity, "A new code was sent. The old code is invalid.", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(this@OTPActivity, it.message ?: "Unable to resend code.", Toast.LENGTH_LONG).show()
            }
            setLoading(false)
        }
    }

    private fun setLoading(loading: Boolean) {
        verifyButton.isEnabled = !loading
        findViewById<View>(R.id.otpProgress).visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_EMAIL = "email"
        const val EXTRA_MODE = "mode"
        const val MODE_SIGN_UP = "sign_up"
        const val MODE_RECOVERY = "recovery"
        private const val OTP_WINDOW_MS = 5 * 60_000L
        private const val MAX_OTP_ATTEMPTS = 5
    }
}
