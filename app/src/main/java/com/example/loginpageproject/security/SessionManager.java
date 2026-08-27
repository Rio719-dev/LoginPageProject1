package com.example.loginpageproject.security;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * PART 30 - Centralized Session Management
 */
public class SessionManager {
    private static final String PREF_NAME = "XenoAuthSession";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_ACCESS_TYPE = "access_type";
    private static final String KEY_LAST_INTERACTION = "last_interaction";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createSession(String email, String accessType) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_ACCESS_TYPE, accessType);
        editor.putLong(KEY_LAST_INTERACTION, System.currentTimeMillis());
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }

    public String getAccessType() {
        return pref.getString(KEY_ACCESS_TYPE, "USER");
    }

    public void updateInteractionTime() {
        editor.putLong(KEY_LAST_INTERACTION, System.currentTimeMillis());
        editor.apply();
    }

    public long getLastInteractionTime() {
        return pref.getLong(KEY_LAST_INTERACTION, 0);
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}