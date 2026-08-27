package com.example.loginpageproject.presentation.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.loginpageproject.BaseActivity;
import com.example.loginpageproject.R;
import com.example.loginpageproject.presentation.viewmodels.LandingViewModel;
import com.example.loginpageproject.presentation.viewmodels.ViewModelFactory;
import com.google.android.material.button.MaterialButton;

public class LandingActivity extends BaseActivity {

    private LandingViewModel viewModel;
    private TextView tvAccessTypeDisplay, tvWelcome;
    private LinearLayout containerRoleSpecific;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(this)).get(LandingViewModel.class);

        tvAccessTypeDisplay = findViewById(R.id.tvAccessTypeDisplay);
        tvWelcome = findViewById(R.id.tvWelcome);
        containerRoleSpecific = findViewById(R.id.containerRoleSpecific);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        MaterialButton btnResetPassword = findViewById(R.id.btnResetPassword);

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnResetPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            intent.putExtra("email", sessionManager.getUserEmail());
            startActivity(intent);
        });

        observeViewModel();
        viewModel.loadUser(sessionManager.getUserEmail());
    }

    private void observeViewModel() {
        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                tvAccessTypeDisplay.setText("ACCESS LEVEL: " + user.getAccessType().toUpperCase());
                tvWelcome.setText("Welcome, " + user.getFullName());
                setupDashboard(user.getAccessType());
            }
        });
    }

    private void setupDashboard(String role) {
        containerRoleSpecific.removeAllViews();
        if ("Super Admin".equalsIgnoreCase(role)) {
            addDashboardItem("Protocol 0: Global Override Active");
            addDashboardItem("Protocol 1: Entity Authorization Module");
            addDashboardItem("Protocol 2: Galactic Firewall Config");
        } else if ("Admin".equalsIgnoreCase(role)) {
            addDashboardItem("Sector Monitoring Uplink");
            addDashboardItem("Entity Registry Review");
        } else {
            addDashboardItem("Personal Log Access");
            addDashboardItem("Identity Matrix Status: Stable");
        }
    }

    private void addDashboardItem(String text) {
        TextView textView = new TextView(this);
        textView.setText(">> " + text);
        textView.setTextColor(getResources().getColor(R.color.alien_star_white));
        textView.setPadding(0, 16, 0, 16);
        textView.setFontFamily(android.graphics.Typeface.MONOSPACE);
        containerRoleSpecific.addView(textView);
    }
}