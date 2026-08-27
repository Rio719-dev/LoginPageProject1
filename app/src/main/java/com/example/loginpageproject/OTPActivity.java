package com.example.loginpageproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class OTPActivity extends BaseActivity {

    private TextInputEditText etOtpCode;
    private TextView tvOtpCountdown;
    private Button btnVerifyOtp, btnResendOtp;
    private CountDownTimer countDownTimer;
    private String generatedOtp = "123456"; // In a real app, this would be random and sent via SMS/Email
    
    private String name, bday, address, email, mobile, password, role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        // Get data from Intent
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        bday = intent.getStringExtra("bday");
        address = intent.getStringExtra("address");
        email = intent.getStringExtra("email");
        mobile = intent.getStringExtra("mobile");
        password = intent.getStringExtra("password");
        role = intent.getStringExtra("role");

        etOtpCode = findViewById(R.id.etOtpCode);
        tvOtpCountdown = findViewById(R.id.tvOtpCountdown);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnResendOtp = findViewById(R.id.btnResendOtp);

        startCountdown();

        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
        btnResendOtp.setOnClickListener(v -> {
            generatedOtp = "654321"; // New fake OTP
            Toast.makeText(this, "New Frequency Code Transmitted", Toast.LENGTH_SHORT).show();
            startCountdown();
            btnResendOtp.setEnabled(false);
        });
    }

    private void startCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvOtpCountdown.setText(String.format(Locale.getDefault(), "Resync available in: 00:%02d", seconds));
            }

            @Override
            public void onFinish() {
                tvOtpCountdown.setText("Resync protocol available.");
                btnResendOtp.setEnabled(true);
            }
        }.start();
    }

    private void verifyOtp() {
        String enteredOtp = etOtpCode.getText().toString();
        if (enteredOtp.equals(generatedOtp)) {
            // Success: Save User to DB
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            User newUser = new User(name, bday, address, email, mobile, password, role);
            if (dbHelper.addUser(newUser)) {
                Toast.makeText(this, "Entity Initialized. Welcome to the Network.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Database Sync Failure", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.error_invalid_otp), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}