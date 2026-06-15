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
import com.budgetquest.app.data.repository.AchievementRepository
import com.budgetquest.app.data.repository.BudgetGoalRepository
import com.budgetquest.app.data.repository.CategoryRepository
import com.budgetquest.app.data.repository.ExpenseRepository
import com.budgetquest.app.data.repository.UnlockedAchievementResult
import com.budgetquest.app.ui.achievements.AchievementUnlockedDialog
import com.budgetquest.app.ui.achievements.AchievementsActivity
import com.budgetquest.app.ui.auth.LoginActivity
import com.budgetquest.app.ui.budget.BudgetActivity
import com.budgetquest.app.ui.category.CategoryActivity
import com.budgetquest.app.ui.charts.SpendingChartActivity
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
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        // ── VIEWS ─────────────────────────────────────────────────────────────
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val cardAddExpense = findViewById<CardView>(R.id.cardAddExpense)
        val cardViewExpenses = findViewById<CardView>(R.id.cardViewExpenses)
        val cardCategories = findViewById<CardView>(R.id.cardCategories)
        val cardBudget = findViewById<CardView>(R.id.cardBudget)
        val cardCategoryTotals = findViewById<CardView>(R.id.cardCategoryTotals)
        val cardAchievements = findViewById<CardView>(R.id.cardAchievements)
        val cardSpendingChart = findViewById<CardView>(R.id.cardSpendingChart)

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
            startActivityForResult(
                Intent(this, CategoryActivity::class.java),
                REQUEST_GENERAL
            )
        }

        cardBudget.setOnClickListener {
            startActivityForResult(
                Intent(this, BudgetActivity::class.java),
                REQUEST_GENERAL
            )
        }

        cardCategoryTotals.setOnClickListener {
            startActivity(Intent(this, CategoryTotalsActivity::class.java))
        }

        cardAchievements.setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }

        cardSpendingChart.setOnClickListener {
            startActivity(Intent(this, SpendingChartActivity::class.java))
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

        // ── DAILY STREAK CHECK ────────────────────────────────────────────────
        lifecycleScope.launch {
            val achievementRepository =
                AchievementRepository(db.achievementDao(), db.userDao())
            val streakUnlocks = withContext(Dispatchers.IO) {
                achievementRepository.updateDailyStreak(userId)
            }
            if (streakUnlocks.isNotEmpty()) {
                AchievementUnlockedDialog.showAll(this@DashboardActivity, streakUnlocks)
            }
        }
    }

    // ── HANDLE RESULTS FROM CHILD ACTIVITIES ──────────────────────────────────
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            val title = data?.getStringExtra("unlocked_achievement_title")
            val desc = data?.getStringExtra("unlocked_achievement_desc")

            if (title != null && desc != null) {
                lifecycleScope.launch {
                    val db = BudgetQuestDatabase.getDatabase(this@DashboardActivity)
                    val achievementRepository =
                        AchievementRepository(db.achievementDao(), db.userDao())
                    val sessionManager = SessionManager(this@DashboardActivity)
                    val userId = sessionManager.getUserId()

                    val all = withContext(Dispatchers.IO) {
                        achievementRepository.getAllAchievements()
                    }
                    val matched = all.find { it.title == title }

                    if (matched != null) {
                        val result = UnlockedAchievementResult(
                            achievement = matched,
                            newLevel = null
                        )
                        AchievementUnlockedDialog.show(this@DashboardActivity, result)
                    }
                    loadDashboardData()
                }
            } else {
                loadDashboardData()
            }
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
        val achievementRepository = AchievementRepository(db.achievementDao(), db.userDao())
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val tvMonthTotal = findViewById<TextView>(R.id.tvMonthTotal)
        val tvBudgetStatus = findViewById<TextView>(R.id.tvBudgetStatus)
        val tvStatExpensesValue = findViewById<TextView>(R.id.tvStatExpensesValue)
        val tvStatCategoriesValue = findViewById<TextView>(R.id.tvStatCategoriesValue)
        val tvStatMonthValue = findViewById<TextView>(R.id.tvStatMonthValue)
        val tvDashLevelCircle = findViewById<TextView>(R.id.tvDashLevelCircle)
        val tvDashLevelLabel = findViewById<TextView>(R.id.tvDashLevelLabel)
        val tvDashBadgeCount = findViewById<TextView>(R.id.tvDashBadgeCount)
        val tvDashStreak = findViewById<TextView>(R.id.tvDashStreak)

        lifecycleScope.launch {

            val monthTotal = withContext(Dispatchers.IO) {
                expenseRepository.getTotalForMonth(userId, monthPrefix)
            }

            val budgetGoal = withContext(Dispatchers.IO) {
                budgetGoalRepository.getBudgetGoal(userId)
            }

            val allExpenses: List<Expense> = withContext(Dispatchers.IO) {
                expenseRepository.getExpensesOnce(userId)
            }

            val categories = withContext(Dispatchers.IO) {
                categoryRepository.getCategoriesOnce(userId)
            }

            val user = withContext(Dispatchers.IO) {
                db.userDao().getUserById(userId)
            }

            val allAchievements = withContext(Dispatchers.IO) {
                achievementRepository.getAllAchievements()
            }

            val unlockedCount = withContext(Dispatchers.IO) {
                achievementRepository.getUnlockedCount(userId)
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

            if (user != null) {
                tvDashLevelCircle.text = user.level.toString()
                val xpInLevel = user.xp % 100
                tvDashLevelLabel.text = "Level ${user.level} · $xpInLevel XP"
                tvDashStreak.text = "🔥 ${user.currentStreak}"
            }

            tvDashBadgeCount.text = "$unlockedCount / ${allAchievements.size} badges"

            // Check if budget goal achievement should be awarded
            if (budgetGoal != null &&
                monthTotal >= budgetGoal.minGoal &&
                monthTotal <= budgetGoal.maxGoal
            ) {
                val unlock = withContext(Dispatchers.IO) {
                    achievementRepository.checkBudgetWithinRangeAchievement(userId)
                }
                if (unlock != null) {
                    AchievementUnlockedDialog.show(this@DashboardActivity, unlock)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_ADD_EXPENSE = 1001
        private const val REQUEST_GENERAL = 1002
    }
}