package com.budgetquest.app.ui.expense

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.budgetquest.app.R
import com.budgetquest.app.data.db.BudgetQuestDatabase
import com.budgetquest.app.data.repository.CategoryRepository
import com.budgetquest.app.data.repository.ExpenseRepository
import com.budgetquest.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ExpenseListActivity : AppCompatActivity() {

    private var collectJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        val db = BudgetQuestDatabase.getDatabase(this)
        val expenseRepository = ExpenseRepository(db.expenseDao())
        val categoryRepository = CategoryRepository(db.categoryDao())
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val btnFromDate = findViewById<Button>(R.id.btnFromDate)
        val btnToDate = findViewById<Button>(R.id.btnToDate)
        val btnApplyFilter = findViewById<Button>(R.id.btnApplyFilter)
        val btnClearFilter = findViewById<Button>(R.id.btnClearFilter)
        val tvFilterError = findViewById<TextView>(R.id.tvFilterError)
        val tvResultCount = findViewById<TextView>(R.id.tvResultCount)
        val rvExpenses = findViewById<RecyclerView>(R.id.rvExpenses)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        var fromDate = ""
        var toDate = ""

        val adapter = ExpenseAdapter(emptyList(), emptyMap())
        rvExpenses.layoutManager = LinearLayoutManager(this)
        rvExpenses.adapter = adapter

        lifecycleScope.launch {
            val categories = withContext(Dispatchers.IO) {
                categoryRepository.getCategoriesOnce(userId)
            }
            val categoryMap = categories.associate { it.id to it.name }

            observeExpenses(
                expenseRepository,
                userId,
                null,
                null,
                categoryMap,
                adapter,
                tvEmpty,
                tvResultCount
            )

            btnFromDate.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    this@ExpenseListActivity,
                    { _, y, m, d ->
                        fromDate = "%04d-%02d-%02d".format(y, m + 1, d)
                        btnFromDate.text = fromDate
                        btnFromDate.setTextColor(
                            ContextCompat.getColor(this@ExpenseListActivity, R.color.black)
                        )
                        tvFilterError.visibility = View.GONE
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            btnToDate.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    this@ExpenseListActivity,
                    { _, y, m, d ->
                        toDate = "%04d-%02d-%02d".format(y, m + 1, d)
                        btnToDate.text = toDate
                        btnToDate.setTextColor(
                            ContextCompat.getColor(this@ExpenseListActivity, R.color.black)
                        )
                        tvFilterError.visibility = View.GONE
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            btnApplyFilter.setOnClickListener {
                tvFilterError.visibility = View.GONE

                if (fromDate.isEmpty() && toDate.isEmpty()) {
                    showFilterError(tvFilterError, "Please select at least one date.")
                    return@setOnClickListener
                }
                if (fromDate.isEmpty()) {
                    showFilterError(tvFilterError, "Please select a From date.")
                    return@setOnClickListener
                }
                if (toDate.isEmpty()) {
                    showFilterError(tvFilterError, "Please select a To date.")
                    return@setOnClickListener
                }
                if (fromDate > toDate) {
                    showFilterError(tvFilterError, "From date cannot be after To date.")
                    return@setOnClickListener
                }

                observeExpenses(
                    expenseRepository,
                    userId,
                    fromDate,
                    toDate,
                    categoryMap,
                    adapter,
                    tvEmpty,
                    tvResultCount
                )
            }

            btnClearFilter.setOnClickListener {
                fromDate = ""
                toDate = ""

                btnFromDate.text = "From Date"
                btnToDate.text = "To Date"

                btnFromDate.setTextColor(
                    ContextCompat.getColor(this@ExpenseListActivity, R.color.text_secondary)
                )
                btnToDate.setTextColor(
                    ContextCompat.getColor(this@ExpenseListActivity, R.color.text_secondary)
                )

                tvFilterError.visibility = View.GONE

                observeExpenses(
                    expenseRepository,
                    userId,
                    null,
                    null,
                    categoryMap,
                    adapter,
                    tvEmpty,
                    tvResultCount
                )
            }
        }
    }

    private fun observeExpenses(
        expenseRepository: ExpenseRepository,
        userId: Int,
        fromDate: String?,
        toDate: String?,
        categoryMap: Map<Int, String>,
        adapter: ExpenseAdapter,
        tvEmpty: TextView,
        tvResultCount: TextView
    ) {
        collectJob?.cancel()

        collectJob = lifecycleScope.launch {
            val flow = if (fromDate != null && toDate != null) {
                expenseRepository.getExpensesByDateRange(userId, fromDate, toDate)
            } else {
                expenseRepository.getExpensesForUser(userId)
            }

            flow.collect { expenses ->
                adapter.updateData(expenses, categoryMap)

                if (expenses.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    tvResultCount.text = ""
                } else {
                    tvEmpty.visibility = View.GONE
                    val label = if (fromDate != null) "Filtered results" else "All expenses"
                    tvResultCount.text = "$label: ${expenses.size} record(s)"
                }
            }
        }
    }

    private fun showFilterError(tv: TextView, message: String) {
        tv.text = message
        tv.visibility = View.VISIBLE
    }
}