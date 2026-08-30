package com.example.loginpageproject

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.loginpageproject.auth.AuthValidator
import com.example.loginpageproject.auth.RegistrationData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlinx.coroutines.launch

class SignUpActivity : BaseActivity() {
    private lateinit var fullName: EditText
    private lateinit var birthday: EditText
    private lateinit var address: EditText
    private lateinit var email: EditText
    private lateinit var username: EditText
    private lateinit var mobile: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var requestedRole: Spinner
    private lateinit var signUpButton: Button
    private lateinit var passwordStrength: TextView
    private lateinit var passwordStrengthBar: ProgressBar
    private lateinit var birthdayValue: LocalDate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        fullName = findViewById(R.id.etFullName)
        birthday = findViewById(R.id.etBirthday)
        address = findViewById(R.id.etAddress)
        email = findViewById(R.id.etEmail)
        username = findViewById(R.id.etUsername)
        mobile = findViewById(R.id.etMobile)
        password = findViewById(R.id.etPassword)
        confirmPassword = findViewById(R.id.etConfirmPassword)
        requestedRole = findViewById(R.id.spnAccessType)
        signUpButton = findViewById(R.id.btnSignUp)
        passwordStrength = findViewById(R.id.tvPasswordStrength)
        passwordStrengthBar = findViewById(R.id.pbPasswordStrength)

        birthday.setOnClickListener { selectBirthday() }
        birthday.keyListener = null
        password.addTextChangedListener(passwordWatcher)
        disableConfirmPasswordPaste()
        signUpButton.setOnClickListener { register() }
        findViewById<TextView>(R.id.tvLoginLink).setOnClickListener { finish() }
    }

    private fun selectBirthday() {
        val maximumDate = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }
        DatePickerDialog(this, { _, year, month, day ->
            birthdayValue = LocalDate.of(year, month + 1, day)
            birthday.setText(birthdayValue.format(DateTimeFormatter.ISO_LOCAL_DATE))
        }, maximumDate.get(Calendar.YEAR), maximumDate.get(Calendar.MONTH), maximumDate.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.maxDate = maximumDate.timeInMillis
        }.show()
    }

    private val passwordWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updatePasswordRequirements(s?.toString().orEmpty())
        override fun afterTextChanged(s: Editable?) = Unit
    }

    private fun updatePasswordRequirements(value: String) {
        val rules = AuthValidator.passwordRequirements(value)
        fun marker(ok: Boolean, requirement: String) = if (ok) "✅ $requirement" else "❌ $requirement"
        findViewById<TextView>(R.id.tvRequirementLength).text = marker(rules.correctLength, "8–16 characters")
        findViewById<TextView>(R.id.tvRequirementUppercase).text = marker(rules.hasUppercase, "Uppercase letter")
        findViewById<TextView>(R.id.tvRequirementLowercase).text = marker(rules.hasLowercase, "Lowercase letter")
        findViewById<TextView>(R.id.tvRequirementNumber).text = marker(rules.hasNumber, "Number")
        findViewById<TextView>(R.id.tvRequirementSpecial).text = marker(rules.hasSpecialCharacter, "Special character")
        passwordStrength.text = passwordStrengthLabel(rules)
        passwordStrengthBar.updatePasswordStrengthBar(rules)
    }

    private fun disableConfirmPasswordPaste() {
        confirmPassword.setLongClickable(false)
        confirmPassword.setTextIsSelectable(false)

        // Blocks BOTH action-mode toolbars: customSelectionActionModeCallback covers a
        // long-press with a text selection, customInsertionActionModeCallback covers a
        // long-press at the blinking cursor with nothing selected (Android shows a
        // "Paste" bubble there too, and the previous version only blocked the first).
        val blockingCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false
            override fun onDestroyActionMode(mode: ActionMode?) = Unit
        }
        confirmPassword.customSelectionActionModeCallback = blockingCallback
        confirmPassword.customInsertionActionModeCallback = blockingCallback

        // Defense-in-depth against paste vectors that never go through either action-mode
        // toolbar above: a hardware-keyboard Ctrl+V, drag-and-drop text, or an autofill/
        // clipboard-suggestion strip inserting a whole string at once. A normal keystroke
        // always inserts exactly one character at a time, so treat any insertion of more
        // than one character in a single edit as a paste and revert it immediately.
        confirmPassword.addTextChangedListener(object : TextWatcher {
            private var previous = ""
            private var isReverting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!isReverting) previous = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isReverting) return
                val current = s?.toString().orEmpty()
                if (current.length - previous.length > 1) {
                    isReverting = true
                    confirmPassword.setText(previous)
                    confirmPassword.setSelection(previous.length)
                    isReverting = false
                }
            }
        })
    }

    private fun register() {
        val enteredName = fullName.text.toString().trim()
        val enteredBirthday = birthday.text.toString().trim()
        val enteredAddress = address.text.toString().trim()
        val enteredEmail = email.text.toString().trim()
        val enteredUsername = username.text.toString().trim()
        val enteredMobile = mobile.text.toString().trim()
        val enteredPassword = password.text.toString()
        val enteredConfirmation = confirmPassword.text.toString()
        val parsedBirthday = AuthValidator.parseBirthday(enteredBirthday)
        when {
            !AuthValidator.isValidFullName(enteredName) -> fullName.error = "Use letters, spaces, hyphens, or apostrophes only"
            parsedBirthday == null || !AuthValidator.isAtLeast18(parsedBirthday) -> birthday.error = "You must be at least 18 years old"
            enteredAddress.isBlank() -> address.error = "Address is required"
            !AuthValidator.isValidEmail(enteredEmail) -> email.error = "Enter a valid email address"
            enteredUsername != enteredEmail -> username.error = "Username must match your email address"
            !AuthValidator.isValidPhilippineMobile(enteredMobile) -> mobile.error = "Use 09XXXXXXXXX or +639XXXXXXXXX"
            !AuthValidator.isStrongPassword(enteredPassword) -> password.error = "Password does not meet all requirements"
            enteredPassword != enteredConfirmation -> confirmPassword.error = "Passwords do not match"
            else -> submitRegistration(enteredName, enteredBirthday, enteredAddress, enteredEmail, enteredMobile, enteredPassword)
        }
    }

    private fun submitRegistration(name: String, birth: String, homeAddress: String, enteredEmail: String, enteredMobile: String, enteredPassword: String) {
        setLoading(true)
        lifecycleScope.launch {
            val existing = runCatching { authRepository.emailExists(enteredEmail) }.getOrElse {
                Toast.makeText(this@SignUpActivity, it.message ?: "Unable to validate email.", Toast.LENGTH_LONG).show()
                setLoading(false)
                return@launch
            }
            if (existing) {
                email.error = "Email already registered. Please use a different email address or reset your password."
                setLoading(false)
                return@launch
            }
            val role = when (requestedRole.selectedItem.toString()) {
                "Super Admin" -> "super_admin"
                "Admin" -> "admin"
                else -> "user"
            }
            runCatching {
                authRepository.register(RegistrationData(name, birth, homeAddress, enteredEmail, enteredMobile, role, enteredPassword))
            }.onSuccess {
                Toast.makeText(this@SignUpActivity, "A verification code was sent to your email.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this@SignUpActivity, OTPActivity::class.java).apply {
                    putExtra(OTPActivity.EXTRA_EMAIL, enteredEmail)
                    putExtra(OTPActivity.EXTRA_MODE, OTPActivity.MODE_SIGN_UP)
                })
                finish()
            }.onFailure {
                Toast.makeText(this@SignUpActivity, it.message ?: "Unable to create account.", Toast.LENGTH_LONG).show()
            }
            setLoading(false)
        }
    }

    private fun setLoading(loading: Boolean) {
        signUpButton.isEnabled = !loading
        findViewById<View>(R.id.signupProgress).visibility = if (loading) View.VISIBLE else View.GONE
    }
}
