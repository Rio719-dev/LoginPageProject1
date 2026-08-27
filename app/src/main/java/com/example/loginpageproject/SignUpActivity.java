package com.example.loginpageproject;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SignUpActivity extends BaseActivity {

    private EditText etFullName, etBirthday, etAddress, etEmail, etMobile, etPassword, etConfirmPassword;
    private Spinner spnAccessType;
    private TextView tvPasswordStrength;
    private Button btnSignUp;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Using activity_main.xml as the Sign-Up layout

        etFullName = findViewById(R.id.etFullName);
        etBirthday = findViewById(R.id.etBirthday);
        etAddress = findViewById(R.id.etAddress);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spnAccessType = findViewById(R.id.spnAccessType);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        // Requirement: Prevent copy-paste in Confirm Password
        etConfirmPassword.setLongClickable(false);
        etConfirmPassword.setTextIsSelectable(false);
        etConfirmPassword.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) { return false; }
            @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
            @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) { return false; }
            @Override public void onDestroyActionMode(ActionMode mode) {}
        });

        // Requirement: Password strength indicator
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSignUp.setOnClickListener(v -> handleSignUp());
        
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            tvPasswordStrength.setText("");
            return;
        }

        int score = 0;
        // 8-16 characters
        if (password.length() >= 8 && password.length() <= 16) score++;
        // Uppercase
        if (password.matches(".*[A-Z].*")) score++;
        // Lowercase
        if (password.matches(".*[a-z].*")) score++;
        // Number
        if (password.matches(".*[0-9].*")) score++;
        // Special character
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score++;

        if (score <= 2) {
            tvPasswordStrength.setText("🔴 " + getString(R.string.password_strength_weak));
            tvPasswordStrength.setTextColor(Color.RED);
        } else if (score < 5) {
            tvPasswordStrength.setText("🟡 " + getString(R.string.password_strength_average));
            tvPasswordStrength.setTextColor(Color.parseColor("#FFD700"));
        } else {
            tvPasswordStrength.setText("🟢 " + getString(R.string.password_strength_strong));
            tvPasswordStrength.setTextColor(Color.GREEN);
        }
    }

    private void handleSignUp() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and Password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Requirement: Check if email is already registered
        if (UserManager.getInstance().isEmailRegistered(email)) {
            Toast.makeText(this, R.string.error_email_exists, Toast.LENGTH_SHORT).show();
            return;
        }

        // Requirement: Match Password and Confirm Password
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.error_password_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        // Requirement: Strength check (must be Green/Strong)
        if (!tvPasswordStrength.getText().toString().contains("🟢")) {
            Toast.makeText(this, R.string.error_password_weak, Toast.LENGTH_SHORT).show();
            return;
        }

        User newUser = new User(
                etFullName.getText().toString(),
                etBirthday.getText().toString(),
                etAddress.getText().toString(),
                email,
                etMobile.getText().toString(),
                password,
                spnAccessType.getSelectedItem().toString()
        );

        if (UserManager.getInstance().registerUser(newUser)) {
            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}