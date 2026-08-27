package com.example.loginpageproject.presentation.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.loginpageproject.BaseActivity;
import com.example.loginpageproject.R;
import com.example.loginpageproject.presentation.viewmodels.ResetPasswordViewModel;
import com.example.loginpageproject.presentation.viewmodels.ViewModelFactory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ResetPasswordActivity extends BaseActivity {

    private ResetPasswordViewModel viewModel;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private TextView tvStrengthIndicator;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        email = getIntent().getStringExtra("email");
        if (email == null) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this, new ViewModelFactory(this)).get(ResetPasswordViewModel.class);

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tvStrengthIndicator = findViewById(R.id.tvStrengthIndicator);
        MaterialButton btnReset = findViewById(R.id.btnReset);

        btnReset.setOnClickListener(v -> {
            viewModel.resetPassword(
                    email,
                    etNewPassword.getText().toString(),
                    etConfirmPassword.getText().toString()
            );
        });

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getPasswordStrength().observe(this, strength -> {
            if (strength <= 2) {
                tvStrengthIndicator.setText("🔴 WEAK: PLASMA LEAK");
                tvStrengthIndicator.setTextColor(getResources().getColor(R.color.alien_plasma_red));
            } else if (strength < 5) {
                tvStrengthIndicator.setText("🟡 STABLE: ION SHIELD");
                tvStrengthIndicator.setTextColor(getResources().getColor(R.color.alien_warning_yellow));
            } else {
                tvStrengthIndicator.setText("🟢 SECURE: QUANTUM CORE");
                tvStrengthIndicator.setTextColor(getResources().getColor(R.color.alien_neon_green));
            }
        });

        viewModel.getStatusAction().observe(this, status -> {
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        });

        viewModel.getNavigateBack().observe(this, navigate -> {
            if (navigate) finish();
        });
    }
}