package com.budgetquest.app.ui.budget

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.budgetquest.app.R
import com.budgetquest.app.data.db.BudgetQuestDatabase
import com.budgetquest.app.data.repository.BudgetGoalRepository
import com.budgetquest.app.data.repository.ExpenseRepository
import com.budgetquest.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        val db = BudgetQuestDatabase.getDatabase(this)
        val budgetGoalRepository = BudgetGoalRepository(db.budgetGoalDao())
        val expenseRepository = ExpenseRepository(db.expenseDao())
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val tvCurrentMin = findViewById<TextView>(R.id.tvCurrentMin)
        val tvCurrentMax = findViewById<TextView>(R.id.tvCurrentMax)
        val tvBudgetStatus = findViewById<TextView>(R.id.tvBudgetStatus)
        val etMinGoal = findViewById<EditText>(R.id.etMinGoal)
        val etMaxGoal = findViewById<EditText>(R.id.etMaxGoal)
        val tvError = findViewById<TextView>(R.id.tvError)
        val tvSuccess = findViewById<TextView>(R.id.tvSuccess)
        val btnSaveGoals = findViewById<Button>(R.id.btnSaveGoals)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            budgetGoalRepository.getBudgetGoalFlow(userId).collect { goal ->
                if (goal != null) {
                    tvCurrentMin.text = "Min Goal: R %.2f".format(goal.minGoal)
                    tvCurrentMax.text = "Max Goal: R %.2f".format(goal.maxGoal)

                    etMinGoal.setText("%.2f".format(goal.minGoal))
                    etMaxGoal.setText("%.2f".format(goal.maxGoal))

                    val totalSpent = withContext(Dispatchers.IO) {
                        expenseRepository.getTotalForMonth(userId, monthPrefix)
                    }

                    tvBudgetStatus.text =
                        buildStatusText(totalSpent, goal.minGoal, goal.maxGoal)

                } else {
                    tvCurrentMin.text = "Min Goal: Not set"
                    tvCurrentMax.text = "Max Goal: Not set"
                    tvBudgetStatus.text = "No budget goals set yet."
                }
            }
        }

        btnSaveGoals.setOnClickListener {
            val minText = etMinGoal.text.toString().trim()
            val maxText = etMaxGoal.text.toString().trim()

            tvError.visibility = View.GONE
            tvSuccess.visibility = View.GONE

            if (minText.isEmpty()) {
                tvError.text = "Minimum goal cannot be empty."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (maxText.isEmpty()) {
                tvError.text = "Maximum goal cannot be empty."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val minGoal = minText.toDoubleOrNull()
            val maxGoal = maxText.toDoubleOrNull()

            if (minGoal == null || minGoal < 0) {
                tvError.text = "Please enter a valid minimum goal."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (maxGoal == null || maxGoal < 0) {
                tvError.text = "Please enter a valid maximum goal."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnSaveGoals.isEnabled = false

            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    budgetGoalRepository.saveOrUpdateGoal(userId, minGoal, maxGoal)
                }

                result.fold(
                    onSuccess = {
                        tvSuccess.text = "Budget goals saved successfully!"
                        tvSuccess.visibility = View.VISIBLE
                        btnSaveGoals.isEnabled = true
                    },
                    onFailure = {
                        tvError.text = it.message ?: "Failed to save goals."
                        tvError.visibility = View.VISIBLE
                        btnSaveGoals.isEnabled = true
                    }
                )
            }
        }
    }

    private fun buildStatusText(
        totalSpent: Double,
        minGoal: Double,
        maxGoal: Double
    ): String {
        val spent = "R %.2f".format(totalSpent)
        return when {
            totalSpent < minGoal ->
                "This month: $spent spent. Under minimum goal — keep going!"
            totalSpent in minGoal..maxGoal ->
                "This month: $spent spent. On track — within your budget range!"
            else ->
                "This month: $spent spent. Over maximum goal — review your spending!"
        }
    }
}