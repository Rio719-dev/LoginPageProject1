package com.example.loginpageproject.presentation.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.loginpageproject.BaseActivity;
import com.example.loginpageproject.R;
import com.example.loginpageproject.domain.model.User;
import com.example.loginpageproject.presentation.viewmodels.OTPViewModel;
import com.example.loginpageproject.presentation.viewmodels.ViewModelFactory;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class OTPActivity extends BaseActivity {

    private OTPViewModel viewModel;
    private TextInputEditText etOtpCode;
    private TextView tvOtpCountdown;
    private Button btnResendOtp;
    private CountDownTimer countDownTimer;
    private User pendingUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(this)).get(OTPViewModel.class);

        // Retrieve pending user data
        Intent intent = getIntent();
        pendingUser = new User(
                intent.getStringExtra("name"),
                intent.getStringExtra("bday"),
                intent.getStringExtra("address"),
                intent.getStringExtra("email"),
                intent.getStringExtra("mobile"),
                intent.getStringExtra("password"),
                intent.getStringExtra("role")
        );

        etOtpCode = findViewById(R.id.etOtpCode);
        tvOtpCountdown = findViewById(R.id.tvOtpCountdown);
        btnResendOtp = findViewById(R.id.btnResendOtp);
        Button btnVerifyOtp = findViewById(R.id.btnVerifyOtp);

        startCountdown();

        btnVerifyOtp.setOnClickListener(v -> {
            String otp = etOtpCode.getText().toString();
            viewModel.verifyOtp(otp, pendingUser);
        });

        btnResendOtp.setOnClickListener(v -> {
            viewModel.resendOtp();
            Toast.makeText(this, "New Frequency Code Transmitted", Toast.LENGTH_SHORT).show();
            startCountdown();
            btnResendOtp.setEnabled(false);
        });

        observeViewModel();
    }

    private void startCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();
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

    private void observeViewModel() {
        viewModel.getRegistrationSuccess().observe(this, success -> {
            if (success) {
                startActivity(new Intent(this, SuccessActivity.class));
                finish();
            }
        });

        viewModel.getError().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}