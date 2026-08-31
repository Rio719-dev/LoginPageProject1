package com.example.loginpageproject

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.loginpageproject.auth.AccessRole
import kotlinx.coroutines.launch

/**
 * Read-only account directory for Admin and Super Admin. Deliberately offers no reset,
 * edit, or role-change action — that authority stays exclusive to Super Admin via
 * UserManagementActivity. Server-side RLS (is_admin_or_super_admin()) is the real
 * enforcement boundary; this screen's own role check only avoids showing a dead end.
 */
class AdminMonitoringActivity : BaseActivity() {
    override val requiresAuthentication = true
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var refreshLayout: SwipeRefreshLayout
    private val adapter = UserMonitorAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_monitoring)
        recyclerView = findViewById(R.id.rvAllUsers)
        emptyState = findViewById(R.id.tvEmptyState)
        refreshLayout = findViewById(R.id.refreshLayout)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        refreshLayout.setOnRefreshListener { loadUsers() }

        lifecycleScope.launch {
            val role = authRepository.currentProfile()?.accessRole
            if (role != AccessRole.ADMIN && role != AccessRole.SUPER_ADMIN) {
                Toast.makeText(this@AdminMonitoringActivity, "Admin access required.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            loadUsers()
        }
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            runCatching { authRepository.listAllUsers() }
                .onSuccess { users ->
                    adapter.submitList(users)
                    emptyState.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (users.isEmpty()) View.GONE else View.VISIBLE
                }
                .onFailure { Toast.makeText(this@AdminMonitoringActivity, it.message ?: "Unable to load users.", Toast.LENGTH_LONG).show() }
            refreshLayout.isRefreshing = false
        }
    }
}
