package com.budgetquest.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.budgetquest.app.R
import com.budgetquest.app.data.db.BudgetQuestDatabase
import com.budgetquest.app.data.repository.CategoryRepository
import com.budgetquest.app.data.repository.ExpenseRepository
import com.budgetquest.app.ui.expense.ExpenseAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminUserExpensesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_user_expenses)

        // Get userId and username passed from AdminDashboardActivity
        val userId = intent.getIntExtra("user_id", -1)
        val username = intent.getStringExtra("username") ?: "Unknown"

        if (userId == -1) {
            finish()
            return
        }

        // Dependencies
        val db = BudgetQuestDatabase.getDatabase(this)
        val expenseRepository = ExpenseRepository(db.expenseDao())
        val categoryRepository = CategoryRepository(db.categoryDao())

        // Views
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val tvUserTitle = findViewById<TextView>(R.id.tvUserTitle)
        val tvUserSummaryTotal = findViewById<TextView>(R.id.tvUserSummaryTotal)
        val tvUserSummaryCount = findViewById<TextView>(R.id.tvUserSummaryCount)
        val rvUserExpenses = findViewById<RecyclerView>(R.id.rvUserExpenses)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        // Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "$username's Expenses"
        toolbar.setNavigationOnClickListener { finish() }

        // Set user title
        tvUserTitle.text = "Expenses for: $username"

        // Adapter — reusing the existing ExpenseAdapter from Step 5
        val adapter = ExpenseAdapter(emptyList(), emptyMap())
        rvUserExpenses.layoutManager = LinearLayoutManager(this)
        rvUserExpenses.adapter = adapter

        // ── LOAD THIS USER'S EXPENSES ─────────────────────────────────────────
        lifecycleScope.launch {
            // Load categories for this user (for name lookup in adapter)
            val categories = withContext(Dispatchers.IO) {
                categoryRepository.getCategoriesOnce(userId)
            }
            val categoryMap = categories.associate { it.id to it.name }

            // Load expenses for this specific user
            val expenses = withContext(Dispatchers.IO) {
                expenseRepository.getExpensesByUserOnce(userId)
            }

            if (expenses.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvUserExpenses.visibility = View.GONE
                tvUserSummaryTotal.text = "Total: R 0.00"
                tvUserSummaryCount.text = "No expenses recorded"
            } else {
                tvEmpty.visibility = View.GONE
                rvUserExpenses.visibility = View.VISIBLE

                val total = expenses.sumOf { it.amount }
                tvUserSummaryTotal.text = "Total: R %.2f".format(total)
                tvUserSummaryCount.text = "${expenses.size} expense(s) recorded"

                adapter.updateData(expenses, categoryMap)
            }
        }
    }
}