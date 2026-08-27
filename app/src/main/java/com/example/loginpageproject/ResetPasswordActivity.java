package com.example.loginpageproject;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;

public class ResetPasswordActivity extends BaseActivity {

    private TextInputEditText etNewPassword, etConfirmPassword;
    private TextView tvStrengthIndicator;
    private Button btnReset;
    private DatabaseHelper dbHelper;
    private String email;
    private boolean fromForgot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        dbHelper = new DatabaseHelper(this);
        email = getIntent().getStringExtra("email");
        fromForgot = getIntent().getBooleanExtra("fromForgot", false);

        if (email == null) {
            finish();
            return;
        }

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tvStrengthIndicator = findViewById(R.id.tvStrengthIndicator);
        btnReset = findViewById(R.id.btnReset);

        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePasswordStrength(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnReset.setOnClickListener(v -> performReset());
    }

    private void validatePasswordStrength(String password) {
        int strength = 0;
        if (password.length() >= 8) strength++;
        if (Pattern.compile("[A-Z]").matcher(password).find()) strength++;
        if (Pattern.compile("[a-z]").matcher(password).find()) strength++;
        if (Pattern.compile("[0-9]").matcher(password).find()) strength++;
        if (Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find()) strength++;

        if (strength <= 2) {
            tvStrengthIndicator.setText(R.string.password_strength_weak);
            tvStrengthIndicator.setTextColor(getResources().getColor(R.color.alien_plasma_red));
        } else if (strength < 5) {
            tvStrengthIndicator.setText(R.string.password_strength_average);
            tvStrengthIndicator.setTextColor(getResources().getColor(R.color.alien_warning_yellow));
        } else {
            tvStrengthIndicator.setText(R.string.password_strength_strong);
            tvStrengthIndicator.setTextColor(getResources().getColor(R.color.alien_neon_green));
        }
    }

    private void performReset() {
        String newPass = etNewPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

        if (newPass.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Protocol Violation: Cipher fields empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirm)) {
            Toast.makeText(this, getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!tvStrengthIndicator.getText().toString().equals(getString(R.string.password_strength_strong))) {
            Toast.makeText(this, "Cipher density too low. Protocol requires QUANTUM CORE strength.", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = dbHelper.getUserByEmail(email);
        if (user != null) {
            // Check password history (reuse prevention)
            if (dbHelper.getUserByEmail(email).getPasswordHistory().contains(newPass)) {
                Toast.makeText(this, getString(R.string.error_password_reuse), Toast.LENGTH_LONG).show();
                return;
            }

            if (dbHelper.updatePassword(email, newPass)) {
                Toast.makeText(this, "Cipher Reprogrammed Successfully.", Toast.LENGTH_LONG).show();
                if (fromForgot) {
                    startActivity(new Intent(this, LoginActivity.class));
                }
                finish();
            } else {
                Toast.makeText(this, "System Failure: Cipher override failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}