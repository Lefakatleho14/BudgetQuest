package com.budgetquest.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.budgetquest.app.R
import com.budgetquest.app.data.db.BudgetQuestDatabase
import com.budgetquest.app.data.repository.ExpenseRepository
import com.budgetquest.app.data.repository.UserRepository
import com.budgetquest.app.ui.auth.LoginActivity
import com.budgetquest.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Dependencies
        val db = BudgetQuestDatabase.getDatabase(this)
        val userRepository = UserRepository(db.userDao())
        val expenseRepository = ExpenseRepository(db.expenseDao())
        val sessionManager = SessionManager(this)

        // Views
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val tvTotalUsers = findViewById<TextView>(R.id.tvTotalUsers)
        val tvTotalExpensesAll = findViewById<TextView>(R.id.tvTotalExpensesAll)
        val tvTotalSpendingAll = findViewById<TextView>(R.id.tvTotalSpendingAll)
        val rvUsers = findViewById<RecyclerView>(R.id.rvUsers)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val btnAdminLogout = findViewById<Button>(R.id.btnAdminLogout)

        // Toolbar — no back arrow since this is a root screen
        setSupportActionBar(toolbar)

        // Adapter
        val adapter = AdminUserAdapter(
            users = emptyList(),
            onViewClick = { userItem ->
                val intent = Intent(this, AdminUserExpensesActivity::class.java)
                intent.putExtra("user_id", userItem.userId)
                intent.putExtra("username", userItem.username)
                startActivity(intent)
            }
        )
        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter = adapter

        // ── LOAD ALL USERS + THEIR STATS ──────────────────────────────────────
        loadAdminData(
            userRepository = userRepository,
            expenseRepository = expenseRepository,
            tvTotalUsers = tvTotalUsers,
            tvTotalExpensesAll = tvTotalExpensesAll,
            tvTotalSpendingAll = tvTotalSpendingAll,
            rvUsers = rvUsers,
            tvEmpty = tvEmpty,
            adapter = adapter
        )

        // ── LOGOUT ────────────────────────────────────────────────────────────
        btnAdminLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    sessionManager.clearSession()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        val db = BudgetQuestDatabase.getDatabase(this)
        val userRepository = UserRepository(db.userDao())
        val expenseRepository = ExpenseRepository(db.expenseDao())
        loadAdminData(
            userRepository = userRepository,
            expenseRepository = expenseRepository,
            tvTotalUsers = findViewById(R.id.tvTotalUsers),
            tvTotalExpensesAll = findViewById(R.id.tvTotalExpensesAll),
            tvTotalSpendingAll = findViewById(R.id.tvTotalSpendingAll),
            rvUsers = findViewById(R.id.rvUsers),
            tvEmpty = findViewById(R.id.tvEmpty),
            adapter = findViewById<RecyclerView>(R.id.rvUsers).adapter as AdminUserAdapter
        )
    }

    private fun loadAdminData(
        userRepository: UserRepository,
        expenseRepository: ExpenseRepository,
        tvTotalUsers: TextView,
        tvTotalExpensesAll: TextView,
        tvTotalSpendingAll: TextView,
        rvUsers: RecyclerView,
        tvEmpty: TextView,
        adapter: AdminUserAdapter
    ) {
        lifecycleScope.launch {
            // Load all normal users (excludes admin accounts)
            val allUsers = withContext(Dispatchers.IO) {
                userRepository.getAllNormalUsers()
            }

            if (allUsers.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvUsers.visibility = View.GONE
                tvTotalUsers.text = "Total Users: 0"
                tvTotalExpensesAll.text = "Total Expenses Across All Users: 0"
                tvTotalSpendingAll.text = "Total Spending Across All Users: R 0.00"
                return@launch
            }

            // For each user, fetch their expenses to calculate count + total
            val adminUserItems = mutableListOf<AdminUserItem>()
            var grandTotalExpenses = 0
            var grandTotalSpending = 0.0

            allUsers.forEach { user ->
                val expenses = withContext(Dispatchers.IO) {
                    expenseRepository.getExpensesByUserOnce(user.id)
                }
                val userTotal = expenses.sumOf { it.amount }
                grandTotalExpenses += expenses.size
                grandTotalSpending += userTotal

                adminUserItems.add(
                    AdminUserItem(
                        userId = user.id,
                        username = user.username,
                        expenseCount = expenses.size,
                        totalSpent = userTotal
                    )
                )
            }

            // Sort by highest spending first
            adminUserItems.sortByDescending { it.totalSpent }

            // Update overview stats
            tvTotalUsers.text = "Total Users: ${allUsers.size}"
            tvTotalExpensesAll.text =
                "Total Expenses Across All Users: $grandTotalExpenses"
            tvTotalSpendingAll.text =
                "Total Spending Across All Users: R %.2f".format(grandTotalSpending)

            // Update list
            tvEmpty.visibility = View.GONE
            rvUsers.visibility = View.VISIBLE
            adapter.updateData(adminUserItems)
        }
    }
}