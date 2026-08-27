package com.example.loginpageproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends BaseActivity {

    private TextInputEditText etEmail;
    private Button btnSendCode;
    private TextView tvBackToLogin;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        dbHelper = new DatabaseHelper(this);
        etEmail = findViewById(R.id.etEmail);
        btnSendCode = findViewById(R.id.btnSendCode);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnSendCode.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Provide a frequency, entity.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.checkEmailExists(email)) {
                // In a real app, send OTP. Here we skip to Reset
                Intent intent = new Intent(this, ResetPasswordActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("fromForgot", true);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Frequency not found in the network.", Toast.LENGTH_SHORT).show();
            }
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }
}