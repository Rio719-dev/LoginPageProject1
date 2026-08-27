package com.example.loginpageproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    private static final long INACTIVITY_TIMEOUT = 60 * 1000; // 1 minute
    private static final Handler logoutHandler = new Handler(Looper.getMainLooper());
    
    private final Runnable logoutRunnable = () -> {
        UserManager.getInstance().logout();
        Intent intent = new Intent(BaseActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!(this instanceof LoginActivity) && !(this instanceof SignUpActivity)) {
            if (UserManager.getInstance().getCurrentUser() == null) {
                redirectToLogin();
            } else {
                startUserInactivityTimer();
            }
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (UserManager.getInstance().getCurrentUser() != null) {
            startUserInactivityTimer();
        }
    }

    private void startUserInactivityTimer() {
        logoutHandler.removeCallbacks(logoutRunnable);
        logoutHandler.postDelayed(logoutRunnable, INACTIVITY_TIMEOUT);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        logoutHandler.removeCallbacks(logoutRunnable);
    }
}