package com.budgetquest.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.budgetquest.app.R

// Holds everything needed to display a user row in the admin list
data class AdminUserItem(
    val userId: Int,
    val username: String,
    val expenseCount: Int,
    val totalSpent: Double
)

class AdminUserAdapter(
    private var users: List<AdminUserItem>,
    private val onViewClick: (AdminUserItem) -> Unit
) : RecyclerView.Adapter<AdminUserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvUserTotalSpent: TextView = itemView.findViewById(R.id.tvUserTotalSpent)
        val tvExpenseCount: TextView = itemView.findViewById(R.id.tvExpenseCount)
        val btnViewExpenses: Button = itemView.findViewById(R.id.btnViewExpenses)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = users[position]
        holder.tvUsername.text = item.username
        holder.tvUserTotalSpent.text = "Total spent: R %.2f".format(item.totalSpent)
        holder.tvExpenseCount.text = "${item.expenseCount} expense(s)"
        holder.btnViewExpenses.setOnClickListener { onViewClick(item) }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newUsers: List<AdminUserItem>) {
        users = newUsers
        notifyDataSetChanged()
    }
}