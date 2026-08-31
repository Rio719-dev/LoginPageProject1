package com.example.loginpageproject

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.loginpageproject.auth.SupabaseAuthRepository
import kotlinx.coroutines.launch

/** Launcher that restores only an explicitly remembered Supabase session. */
class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lifecycleScope.launch {
            val destination = runCatching {
                if (!secureSessionStore.rememberMe) authRepository.signOut()
                if (secureSessionStore.rememberMe && authRepository.currentProfile() != null) {
                    LandingActivity::class.java
                } else {
                    LoginActivity::class.java
                }
            }.getOrDefault(LoginActivity::class.java)
            startActivity(Intent(this@MainActivity, destination))
            finish()
        }
    }
}
