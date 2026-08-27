package com.example.loginpageproject.presentation.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.loginpageproject.BaseActivity;
import com.example.loginpageproject.R;
import com.example.loginpageproject.presentation.viewmodels.ForgotPasswordViewModel;
import com.example.loginpageproject.presentation.viewmodels.ViewModelFactory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends BaseActivity {

    private ForgotPasswordViewModel viewModel;
    private TextInputEditText etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(this)).get(ForgotPasswordViewModel.class);

        etEmail = findViewById(R.id.etEmail);
        MaterialButton btnSendCode = findViewById(R.id.btnSendCode);
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnSendCode.setOnClickListener(v -> {
            viewModel.sendResetCode(etEmail.getText().toString().trim());
        });

        tvBackToLogin.setOnClickListener(v -> finish());

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getStatusAction().observe(this, status -> {
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        });

        viewModel.getNavigateToReset().observe(this, email -> {
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            intent.putExtra("email", email);
            intent.putExtra("fromForgot", true);
            startActivity(intent);
            finish();
        });
    }
}