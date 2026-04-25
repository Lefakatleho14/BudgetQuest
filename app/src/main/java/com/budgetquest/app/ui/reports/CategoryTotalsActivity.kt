package com.budgetquest.app.ui.reports

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
import com.budgetquest.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryTotalsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_totals)

        val db = BudgetQuestDatabase.getDatabase(this)
        val expenseRepository = ExpenseRepository(db.expenseDao())
        val categoryRepository = CategoryRepository(db.categoryDao())
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val tvGrandTotal = findViewById<TextView>(R.id.tvGrandTotal)
        val tvCategoryCount = findViewById<TextView>(R.id.tvCategoryCount)
        val rvCategoryTotals = findViewById<RecyclerView>(R.id.rvCategoryTotals)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val adapter = CategoryTotalAdapter(emptyList())
        rvCategoryTotals.layoutManager = LinearLayoutManager(this)
        rvCategoryTotals.adapter = adapter

        lifecycleScope.launch {

            val categories = withContext(Dispatchers.IO) {
                categoryRepository.getCategoriesOnce(userId)
            }

            val categoryMap = categories.associate { it.id to it.name }

            val rawTotals = withContext(Dispatchers.IO) {
                expenseRepository.getCategoryTotals(userId)
            }

            if (rawTotals.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvCategoryTotals.visibility = View.GONE
                tvGrandTotal.text = "R 0.00"
                tvCategoryCount.text = "0 categories"
                return@launch
            }

            val grandTotal = rawTotals.sumOf { it.total }

            val items = rawTotals
                .sortedByDescending { it.total }
                .map { categoryTotal ->
                    val name = categoryMap[categoryTotal.category_id] ?: "Uncategorised"

                    val percentage = if (grandTotal > 0) {
                        ((categoryTotal.total / grandTotal) * 100).toInt()
                    } else {
                        0
                    }

                    CategoryTotalItem(
                        categoryName = name,
                        total = categoryTotal.total,
                        percentage = percentage
                    )
                }

            tvGrandTotal.text = "R %.2f".format(grandTotal)
            tvCategoryCount.text =
                "${items.size} categor${if (items.size == 1) "y" else "ies"}"

            tvEmpty.visibility = View.GONE
            rvCategoryTotals.visibility = View.VISIBLE
            adapter.updateData(items)
        }
    }
}