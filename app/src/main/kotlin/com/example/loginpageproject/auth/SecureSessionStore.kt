package com.example.loginpageproject.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Stores only the user's opt-in remember-me preference in encrypted local storage. */
class SecureSessionStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "secure_auth_preferences",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var rememberMe: Boolean
        get() = preferences.getBoolean(KEY_REMEMBER_ME, false)
        set(value) = preferences.edit().putBoolean(KEY_REMEMBER_ME, value).apply()

    fun clear() = preferences.edit().clear().apply()

    private companion object {
        const val KEY_REMEMBER_ME = "remember_me"
    }
}
