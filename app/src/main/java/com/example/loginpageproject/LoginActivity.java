package com.example.loginpageproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends BaseActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignUpLink, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUpLink = findViewById(R.id.tvSignUpLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            User user = UserManager.getInstance().login(email, password);
            if (user != null) {
                Intent intent = new Intent(LoginActivity.this, LandingActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, R.string.error_invalid_login, Toast.LENGTH_SHORT).show();
            }
        });

        tvSignUpLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
        
        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot Password feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }
}