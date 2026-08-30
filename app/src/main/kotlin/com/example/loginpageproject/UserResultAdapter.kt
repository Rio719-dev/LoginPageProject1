package com.example.loginpageproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.loginpageproject.auth.AccessRole
import com.example.loginpageproject.auth.UserProfile

/**
 * Renders search results in the Super Admin user-management screen with per-row
 * actions: reset password, promote/demote Admin access, and delete the account.
 * The Super Admin's own row (there is exactly one) never shows these actions,
 * since the backing RPCs reject self-targeting and targeting the Super Admin role.
 */
class UserResultAdapter(
    private val onResetClicked: (UserProfile) -> Unit,
    private val onToggleAdminClicked: (UserProfile) -> Unit,
    private val onDeleteClicked: (UserProfile) -> Unit
) : RecyclerView.Adapter<UserResultAdapter.UserViewHolder>() {

    private val users = mutableListOf<UserProfile>()

    fun submitList(newUsers: List<UserProfile>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_result, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], onResetClicked, onToggleAdminClicked, onDeleteClicked)
    }

    override fun getItemCount(): Int = users.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.tvUserName)
        private val email: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val role: TextView = itemView.findViewById(R.id.tvUserRole)
        private val resetButton: ImageButton = itemView.findViewById(R.id.btnResetThisUser)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteThisUser)
        private val toggleAdminButton: Button = itemView.findViewById(R.id.btnToggleAdmin)

        fun bind(
            user: UserProfile,
            onResetClicked: (UserProfile) -> Unit,
            onToggleAdminClicked: (UserProfile) -> Unit,
            onDeleteClicked: (UserProfile) -> Unit
        ) {
            name.text = user.fullName
            email.text = user.email
            role.text = user.accessRole.displayName
            resetButton.setOnClickListener { onResetClicked(user) }

            val isSuperAdmin = user.accessRole == AccessRole.SUPER_ADMIN
            deleteButton.visibility = if (isSuperAdmin) View.GONE else View.VISIBLE
            deleteButton.setOnClickListener { onDeleteClicked(user) }

            toggleAdminButton.visibility = if (isSuperAdmin) View.GONE else View.VISIBLE
            toggleAdminButton.text = itemView.context.getString(
                if (user.accessRole == AccessRole.ADMIN) R.string.btn_demote_to_user else R.string.btn_promote_to_admin
            )
            toggleAdminButton.setOnClickListener { onToggleAdminClicked(user) }
        }
    }
}
