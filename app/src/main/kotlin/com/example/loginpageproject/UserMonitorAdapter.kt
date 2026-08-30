package com.example.loginpageproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.loginpageproject.auth.UserProfile

/**
 * Renders the read-only user directory for Admin monitoring. Deliberately has no reset
 * or edit action — Admins can view but never modify accounts; only Super Admin can
 * initiate a password reset (see UserManagementActivity / UserResultAdapter).
 */
class UserMonitorAdapter : RecyclerView.Adapter<UserMonitorAdapter.UserViewHolder>() {

    private val users = mutableListOf<UserProfile>()

    fun submitList(newUsers: List<UserProfile>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_readonly, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.tvUserName)
        private val email: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val role: TextView = itemView.findViewById(R.id.tvUserRole)

        fun bind(user: UserProfile) {
            name.text = user.fullName
            email.text = user.email
            role.text = user.accessRole.displayName
        }
    }
}
