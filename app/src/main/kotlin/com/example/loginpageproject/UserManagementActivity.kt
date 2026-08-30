package com.example.loginpageproject

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loginpageproject.auth.AccessRole
import com.example.loginpageproject.auth.UserProfile
import kotlinx.coroutines.launch

class UserManagementActivity : BaseActivity() {
    override val requiresAuthentication = true
    private lateinit var query: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var adapter: UserResultAdapter
    private var lastQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)
        query = findViewById(R.id.etUserSearch)
        recyclerView = findViewById(R.id.rvUserResults)
        emptyState = findViewById(R.id.tvEmptyState)

        adapter = UserResultAdapter(
            onResetClicked = ::initiateReset,
            onToggleAdminClicked = ::confirmToggleAdmin,
            onDeleteClicked = ::confirmDelete
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSearchUsers).setOnClickListener { search() }

        lifecycleScope.launch {
            if (authRepository.currentProfile()?.accessRole != AccessRole.SUPER_ADMIN) {
                Toast.makeText(this@UserManagementActivity, "Super Admin access required.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun search() {
        val text = query.text.toString().trim()
        if (text.isBlank()) {
            query.error = "Enter an email to search"
            return
        }
        lastQuery = text
        lifecycleScope.launch {
            runCatching { authRepository.searchUsers(text) }
                .onSuccess { users ->
                    adapter.submitList(users)
                    emptyState.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (users.isEmpty()) View.GONE else View.VISIBLE
                }
                .onFailure { Toast.makeText(this@UserManagementActivity, it.message ?: "Unable to search users.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun refreshSearch() {
        if (lastQuery.isNotBlank()) search()
    }

    private fun initiateReset(user: UserProfile) {
        lifecycleScope.launch {
            runCatching { authRepository.initiateAdminReset(user.id, user.email) }
                .onSuccess { Toast.makeText(this@UserManagementActivity, "Password reset code sent to ${user.email}.", Toast.LENGTH_LONG).show() }
                .onFailure { Toast.makeText(this@UserManagementActivity, it.message ?: "Unable to initiate reset.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun confirmToggleAdmin(user: UserProfile) {
        val promoting = user.accessRole != AccessRole.ADMIN
        val newRole = if (promoting) AccessRole.ADMIN else AccessRole.USER
        val title = getString(if (promoting) R.string.confirm_promote_title else R.string.confirm_demote_title)
        val message = getString(
            if (promoting) R.string.confirm_promote_message else R.string.confirm_demote_message,
            user.fullName
        )
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.btn_confirm) { _, _ -> setRole(user, newRole) }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun setRole(user: UserProfile, newRole: AccessRole) {
        lifecycleScope.launch {
            runCatching { authRepository.setUserRole(user.id, newRole) }
                .onSuccess {
                    Toast.makeText(this@UserManagementActivity, "${user.fullName} is now ${newRole.displayName}.", Toast.LENGTH_LONG).show()
                    refreshSearch()
                }
                .onFailure { Toast.makeText(this@UserManagementActivity, it.message ?: "Unable to update role.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun confirmDelete(user: UserProfile) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_user_title)
            .setMessage(getString(R.string.confirm_delete_user_message, user.fullName))
            .setPositiveButton(R.string.btn_delete_user) { _, _ -> deleteUser(user) }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun deleteUser(user: UserProfile) {
        lifecycleScope.launch {
            runCatching { authRepository.deleteUser(user.id) }
                .onSuccess {
                    Toast.makeText(this@UserManagementActivity, "${user.fullName}'s account was deleted.", Toast.LENGTH_LONG).show()
                    refreshSearch()
                }
                .onFailure { Toast.makeText(this@UserManagementActivity, it.message ?: "Unable to delete account.", Toast.LENGTH_LONG).show() }
        }
    }
}
