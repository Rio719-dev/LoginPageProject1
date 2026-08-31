package com.example.loginpageproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.loginpageproject.auth.AccessRole
import com.example.loginpageproject.auth.UserProfile
import com.example.loginpageproject.theme.ThemeSettings
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class LandingActivity : BaseActivity() {
    override val requiresAuthentication = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        val themeToggle = findViewById<MaterialSwitch>(R.id.swThemeToggle)
        themeToggle.isChecked = ThemeSettings.isDarkMode(this)
        themeToggle.setOnCheckedChangeListener { _, isChecked ->
            ThemeSettings.setDarkMode(this, isChecked)
            recreate()
        }

        findViewById<ImageButton>(R.id.btnLogout).setOnClickListener { logout() }
        findViewById<MaterialCardView>(R.id.btnResetPassword).setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java).putExtra(ResetPasswordActivity.EXTRA_MODE, ResetPasswordActivity.MODE_AUTHENTICATED))
        }
        findViewById<MaterialCardView>(R.id.btnUserManagement).setOnClickListener {
            startActivity(Intent(this, UserManagementActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.btnAdminMonitoring).setOnClickListener {
            startActivity(Intent(this, AdminMonitoringActivity::class.java))
        }
    }

    override fun onAuthenticatedProfileAvailable(profile: UserProfile) {
        findViewById<TextView>(R.id.tvAccessTypeDisplay).text = getString(R.string.access_type_prefix) + profile.accessRole.displayName
        findViewById<TextView>(R.id.tvWelcome).text = "Welcome, ${profile.fullName}"
        findViewById<TextView>(R.id.tvRoleFeatures).text = when (profile.accessRole) {
            AccessRole.SUPER_ADMIN -> "User management, account management, role management, and system administration are available."
            AccessRole.ADMIN -> "Administrative functions assigned by the system are available."
            AccessRole.USER -> "Standard user functionality is available."
        }
        findViewById<View>(R.id.sectionAdmin).visibility =
            if (profile.accessRole == AccessRole.ADMIN || profile.accessRole == AccessRole.SUPER_ADMIN) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btnUserManagement).visibility =
            if (profile.accessRole == AccessRole.SUPER_ADMIN) View.VISIBLE else View.GONE

        if (profile.mustChangePassword) {
            Toast.makeText(this, "An administrator requires you to reset your password.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, ResetPasswordActivity::class.java).putExtra(ResetPasswordActivity.EXTRA_MODE, ResetPasswordActivity.MODE_AUTHENTICATED))
        }
    }

    private fun logout() {
        cancelInactivityTimer()
        lifecycleScope.launch {
            authRepository.signOut()
            secureSessionStore.clear()
            redirectToLogin(null)
        }
    }
}
