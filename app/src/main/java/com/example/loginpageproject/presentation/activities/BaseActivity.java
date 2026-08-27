package com.example.loginpageproject.presentation.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.example.loginpageproject.security.SessionManager;

/**
 * PART 29 - Automatic Logout Protocol
 * Centralized inactivity timer for all protected command modules.
 */
public abstract class BaseActivity extends AppCompatActivity {
    protected SessionManager sessionManager;
    private static final long INACTIVITY_TIMEOUT = 60000; // 1 minute in milliseconds
    private static final Handler inactivityHandler = new Handler(Looper.getMainLooper());
    
    private final Runnable logoutRunnable = () -> {
        if (sessionManager.isLoggedIn()) {
            sessionManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("reason", "inactivity_timeout");
            startActivity(intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetInactivityTimer();
    }

    protected void resetInactivityTimer() {
        inactivityHandler.removeCallbacks(logoutRunnable);
        if (sessionManager.isLoggedIn()) {
            inactivityHandler.postDelayed(logoutRunnable, INACTIVITY_TIMEOUT);
        }
    }

    protected void stopInactivityTimer() {
        inactivityHandler.removeCallbacks(logoutRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) {
            resetInactivityTimer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // We stop the timer when the app is in the background to avoid 
        // logging out while the user is away but the app is still "open" 
        // in their mind. However, Requirement 29 says "inactivity with the application".
        // Usually, this implies while the app is foregrounded.
        stopInactivityTimer();
    }
}