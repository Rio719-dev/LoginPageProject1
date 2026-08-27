package com.example.loginpageproject.presentation.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.example.loginpageproject.BaseActivity;
import com.example.loginpageproject.R;
import com.google.android.material.button.MaterialButton;

public class SuccessActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        String role = getIntent().getStringExtra("role");
        TextView tvAccessType = findViewById(R.id.tvSuccessAccessType);
        if (role != null) {
            tvAccessType.setText("ACCESS TYPE: " + role.toUpperCase());
        }

        MaterialButton btnLogin = findViewById(R.id.btnGoToLogin);
        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // subtle pulse animation for the icon
        Animation pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        pulse.setDuration(1000);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setRepeatMode(Animation.REVERSE);
        findViewById(R.id.ivSuccessIcon).startAnimation(pulse);
    }
}