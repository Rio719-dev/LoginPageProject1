package com.example.loginpageproject

import android.app.Application
import com.example.loginpageproject.theme.ThemeSettings

class AuthApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeSettings.applySavedTheme(this)
    }
}
