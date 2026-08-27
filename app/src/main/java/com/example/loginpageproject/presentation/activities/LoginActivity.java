package com.example.loginpageproject.presentation.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.loginpageproject.BaseActivity;
import com.example.loginpageproject.R;
import com.example.loginpageproject.presentation.viewmodels.LoginViewModel;
import com.example.loginpageproject.presentation.viewmodels.ViewModelFactory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends BaseActivity {

    private LoginViewModel viewModel;
    private TextInputEditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(this)).get(LoginViewModel.class);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvSignUp = findViewById(R.id.tvSignUp);

        if (getIntent().hasExtra("reason") && "inactivity".equals(getIntent().getStringExtra("reason"))) {
            Toast.makeText(this, getString(R.string.msg_inactivity), Toast.LENGTH_LONG).show();
        }

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            viewModel.login(email, password);
        });

        tvSignUp.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));
        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getLoginSuccess().observe(this, user -> {
            sessionManager.createSession(user.getEmail());
            Toast.makeText(this, "Uplink Established. Welcome, " + user.getFullName(), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LandingActivity.class));
            finish();
        });

        viewModel.getLoginError().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }
}