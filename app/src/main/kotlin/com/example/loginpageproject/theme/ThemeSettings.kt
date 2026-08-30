package com.example.loginpageproject.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeSettings {
    private const val PREFS = "theme_settings"
    private const val KEY_DARK_MODE = "dark_mode"

    fun applySavedTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode(context)) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun isDarkMode(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK_MODE, false)

    fun setDarkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /** Flips the persisted theme and returns the new state. Recreating the activity applies it visually. */
    fun toggleDarkMode(context: Context): Boolean {
        val next = !isDarkMode(context)
        setDarkMode(context, next)
        return next
    }
}
