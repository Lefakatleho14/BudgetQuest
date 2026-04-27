package com.budgetquest.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.budgetquest.app.R
import com.budgetquest.app.data.db.BudgetQuestDatabase
import com.budgetquest.app.data.db.entity.Expense
import com.budgetquest.app.data.repository.BudgetGoalRepository
import com.budgetquest.app.data.repository.CategoryRepository
import com.budgetquest.app.data.repository.ExpenseRepository
import com.budgetquest.app.ui.auth.LoginActivity
import com.budgetquest.app.ui.budget.BudgetActivity
import com.budgetquest.app.ui.category.CategoryActivity
import com.budgetquest.app.ui.expense.AddExpenseActivity
import com.budgetquest.app.ui.expense.ExpenseListActivity
import com.budgetquest.app.ui.reports.CategoryTotalsActivity
import com.budgetquest.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // ── DEPENDENCIES ──────────────────────────────────────────────────────
        val db = BudgetQuestDatabase.getDatabase(this)
        val expenseRepository = ExpenseRepository(db.expenseDao())
        val categoryRepository = CategoryRepository(db.categoryDao())
        val budgetGoalRepository = BudgetGoalRepository(db.budgetGoalDao())
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        // ── VIEWS ─────────────────────────────────────────────────────────────
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvMonthTotal = findViewById<TextView>(R.id.tvMonthTotal)
        val tvBudgetStatus = findViewById<TextView>(R.id.tvBudgetStatus)
        val tvStatExpensesValue = findViewById<TextView>(R.id.tvStatExpensesValue)
        val tvStatCategoriesValue = findViewById<TextView>(R.id.tvStatCategoriesValue)
        val tvStatMonthValue = findViewById<TextView>(R.id.tvStatMonthValue)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val cardAddExpense = findViewById<CardView>(R.id.cardAddExpense)
        val cardViewExpenses = findViewById<CardView>(R.id.cardViewExpenses)
        val cardCategories = findViewById<CardView>(R.id.cardCategories)
        val cardBudget = findViewById<CardView>(R.id.cardBudget)
        val cardCategoryTotals = findViewById<CardView>(R.id.cardCategoryTotals)

        // ── WELCOME ───────────────────────────────────────────────────────────
        val username = sessionManager.getUsername()
        tvWelcome.text = "Welcome back, $username!"

        // ── NAVIGATION ────────────────────────────────────────────────────────
        cardAddExpense.setOnClickListener {
            startActivityForResult(
                Intent(this, AddExpenseActivity::class.java),
                REQUEST_ADD_EXPENSE
            )
        }

        cardViewExpenses.setOnClickListener {
            startActivity(Intent(this, ExpenseListActivity::class.java))
        }

        cardCategories.setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }

        cardBudget.setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
        }

        cardCategoryTotals.setOnClickListener {
            startActivity(Intent(this, CategoryTotalsActivity::class.java))
        }

        // ── LOGOUT ────────────────────────────────────────────────────────────
        btnLogout.setOnClickListener {
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

    // ── REFRESH AFTER RETURNING FROM ADD EXPENSE ──────────────────────────────
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_EXPENSE && resultCode == RESULT_OK) {
            loadDashboardData()
        }
    }

    // ── REFRESH ON EVERY RESUME ───────────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    // ── LOAD ALL STATS ────────────────────────────────────────────────────────
    private fun loadDashboardData() {
        val db = BudgetQuestDatabase.getDatabase(this)
        val expenseRepository = ExpenseRepository(db.expenseDao())
        val categoryRepository = CategoryRepository(db.categoryDao())
        val budgetGoalRepository = BudgetGoalRepository(db.budgetGoalDao())
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val tvMonthTotal = findViewById<TextView>(R.id.tvMonthTotal)
        val tvBudgetStatus = findViewById<TextView>(R.id.tvBudgetStatus)
        val tvStatExpensesValue = findViewById<TextView>(R.id.tvStatExpensesValue)
        val tvStatCategoriesValue = findViewById<TextView>(R.id.tvStatCategoriesValue)
        val tvStatMonthValue = findViewById<TextView>(R.id.tvStatMonthValue)

        lifecycleScope.launch {

            // ── THIS MONTH TOTAL ──────────────────────────────────────────────
            val monthTotal = withContext(Dispatchers.IO) {
                expenseRepository.getTotalForMonth(userId, monthPrefix)
            }

            // ── BUDGET GOAL ───────────────────────────────────────────────────
            val budgetGoal = withContext(Dispatchers.IO) {
                budgetGoalRepository.getBudgetGoal(userId)
            }

            // ── ALL EXPENSES (one-shot, safe on IO thread) ────────────────────
            val allExpenses: List<Expense> = withContext(Dispatchers.IO) {
                expenseRepository.getExpensesOnce(userId)
            }

            // ── CATEGORIES ────────────────────────────────────────────────────
            val categories = withContext(Dispatchers.IO) {
                categoryRepository.getCategoriesOnce(userId)
            }

            // ── UPDATE UI ─────────────────────────────────────────────────────
            tvMonthTotal.text = "R %.2f".format(monthTotal)

            tvBudgetStatus.text = if (budgetGoal != null) {
                when {
                    monthTotal < budgetGoal.minGoal ->
                        "Under minimum goal (R %.2f)".format(budgetGoal.minGoal)
                    monthTotal <= budgetGoal.maxGoal ->
                        "On track — within budget range ✓"
                    else ->
                        "⚠ Over maximum goal (R %.2f)".format(budgetGoal.maxGoal)
                }
            } else {
                "No budget goals set — tap Budget Goals to add one."
            }

            tvStatExpensesValue.text = allExpenses.size.toString()
            tvStatCategoriesValue.text = categories.size.toString()

            val thisMonthCount = allExpenses.count { it.date.startsWith(monthPrefix) }
            tvStatMonthValue.text = thisMonthCount.toString()
        }
    }

    companion object {
        private const val REQUEST_ADD_EXPENSE = 1001
    }
}