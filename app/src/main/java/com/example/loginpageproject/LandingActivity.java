package com.example.loginpageproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LandingActivity extends BaseActivity {

    private TextView tvAccessTypeDisplay;
    private Button btnLogout, btnResetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        tvAccessTypeDisplay = findViewById(R.id.tvAccessTypeDisplay);
        btnLogout = findViewById(R.id.btnLogout);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        User currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Requirement: Display Access Type in top-left
            tvAccessTypeDisplay.setText("Access Type: " + currentUser.accessType);
        }

        btnLogout.setOnClickListener(v -> {
            UserManager.getInstance().logout();
            startActivity(new Intent(LandingActivity.this, LoginActivity.class));
            finish();
        });

        btnResetPassword.setOnClickListener(v -> showResetPasswordDialog());
    }

    private void showResetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_reset_password, null);
        EditText etNewPassword = view.findViewById(R.id.etNewPassword);
        Button btnSave = view.findViewById(R.id.btnSavePassword);

        builder.setView(view);
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String newPassword = etNewPassword.getText().toString();
            User user = UserManager.getInstance().getCurrentUser();

            // Requirement: Password Strength Validation
            if (!isPasswordStrong(newPassword)) {
                Toast.makeText(this, "Password must be 8-16 characters with Upper, Lower, Number, and Special character.", Toast.LENGTH_LONG).show();
                return;
            }

            // Requirement: Prevent Password Reuse
            if (UserManager.getInstance().isPasswordReused(user, newPassword)) {
                Toast.makeText(this, getString(R.string.error_password_reuse), Toast.LENGTH_SHORT).show();
                return;
            }

            UserManager.getInstance().updatePassword(user, newPassword);
            Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private boolean isPasswordStrong(String password) {
        return password.length() >= 8 && password.length() <= 16 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*[0-9].*") &&
                password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    }
}