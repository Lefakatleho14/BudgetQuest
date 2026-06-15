package com.budgetquest.app.ui.charts

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.budgetquest.app.R
import com.budgetquest.app.data.db.BudgetQuestDatabase
import com.budgetquest.app.data.repository.BudgetGoalRepository
import com.budgetquest.app.data.repository.CategoryRepository
import com.budgetquest.app.data.repository.ExpenseRepository
import com.budgetquest.app.utils.SessionManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SpendingChartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spending_chart)

        // Dependencies
        val db = BudgetQuestDatabase.getDatabase(this)
        val expenseRepository = ExpenseRepository(db.expenseDao())
        val categoryRepository = CategoryRepository(db.categoryDao())
        val budgetGoalRepository = BudgetGoalRepository(db.budgetGoalDao())
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        // Views
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val btnFromDate = findViewById<Button>(R.id.btnFromDate)
        val btnToDate = findViewById<Button>(R.id.btnToDate)
        val btnApply = findViewById<Button>(R.id.btnApply)
        val tvFilterError = findViewById<TextView>(R.id.tvFilterError)
        val cardGoalInfo = findViewById<CardView>(R.id.cardGoalInfo)
        val cardChart = findViewById<CardView>(R.id.cardChart)
        val cardBudgetProgress = findViewById<CardView>(R.id.cardBudgetProgress)
        val tvNoData = findViewById<TextView>(R.id.tvNoData)
        val tvGoalMin = findViewById<TextView>(R.id.tvGoalMin)
        val tvGoalMax = findViewById<TextView>(R.id.tvGoalMax)
        val tvGoalStatus = findViewById<TextView>(R.id.tvGoalStatus)
        val tvChartSubtitle = findViewById<TextView>(R.id.tvChartSubtitle)
        val barChart = findViewById<BarChart>(R.id.barChart)
        val tvSpentAmount = findViewById<TextView>(R.id.tvSpentAmount)
        val tvProgressPercent = findViewById<TextView>(R.id.tvProgressPercent)
        val progressBudget = findViewById<ProgressBar>(R.id.progressBudget)
        val tvMinLabel = findViewById<TextView>(R.id.tvMinLabel)
        val tvMaxLabel = findViewById<TextView>(R.id.tvMaxLabel)
        val tvProgressStatus = findViewById<TextView>(R.id.tvProgressStatus)

        // Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Default date range — current month
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Date()
        val calendar = Calendar.getInstance()

        // Default from = first day of current month
        calendar.time = today
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        var fromDate = displayFormat.format(calendar.time)

        // Default to = today
        var toDate = displayFormat.format(today)

        // Set button labels to defaults
        btnFromDate.text = fromDate
        btnFromDate.setTextColor(ContextCompat.getColor(this, R.color.black))
        btnToDate.text = toDate
        btnToDate.setTextColor(ContextCompat.getColor(this, R.color.black))

        // ── FROM DATE PICKER ──────────────────────────────────────────────────
        btnFromDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    fromDate = "%04d-%02d-%02d".format(year, month + 1, day)
                    btnFromDate.text = fromDate
                    btnFromDate.setTextColor(
                        ContextCompat.getColor(this, R.color.black)
                    )
                    tvFilterError.visibility = View.GONE
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // ── TO DATE PICKER ────────────────────────────────────────────────────
        btnToDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    toDate = "%04d-%02d-%02d".format(year, month + 1, day)
                    btnToDate.text = toDate
                    btnToDate.setTextColor(
                        ContextCompat.getColor(this, R.color.black)
                    )
                    tvFilterError.visibility = View.GONE
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Auto-generate chart for current month on screen open
        generateChart(
            userId = userId,
            fromDate = fromDate,
            toDate = toDate,
            expenseRepository = expenseRepository,
            categoryRepository = categoryRepository,
            budgetGoalRepository = budgetGoalRepository,
            barChart = barChart,
            cardChart = cardChart,
            cardGoalInfo = cardGoalInfo,
            cardBudgetProgress = cardBudgetProgress,
            tvNoData = tvNoData,
            tvChartSubtitle = tvChartSubtitle,
            tvGoalMin = tvGoalMin,
            tvGoalMax = tvGoalMax,
            tvGoalStatus = tvGoalStatus,
            tvSpentAmount = tvSpentAmount,
            tvProgressPercent = tvProgressPercent,
            progressBudget = progressBudget,
            tvMinLabel = tvMinLabel,
            tvMaxLabel = tvMaxLabel,
            tvProgressStatus = tvProgressStatus,
            tvFilterError = tvFilterError
        )

        // ── APPLY FILTER ──────────────────────────────────────────────────────
        btnApply.setOnClickListener {
            tvFilterError.visibility = View.GONE

            if (fromDate.isEmpty() || toDate.isEmpty()) {
                tvFilterError.text = "Please select both dates."
                tvFilterError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (fromDate > toDate) {
                tvFilterError.text = "From date cannot be after To date."
                tvFilterError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            generateChart(
                userId = userId,
                fromDate = fromDate,
                toDate = toDate,
                expenseRepository = expenseRepository,
                categoryRepository = categoryRepository,
                budgetGoalRepository = budgetGoalRepository,
                barChart = barChart,
                cardChart = cardChart,
                cardGoalInfo = cardGoalInfo,
                cardBudgetProgress = cardBudgetProgress,
                tvNoData = tvNoData,
                tvChartSubtitle = tvChartSubtitle,
                tvGoalMin = tvGoalMin,
                tvGoalMax = tvGoalMax,
                tvGoalStatus = tvGoalStatus,
                tvSpentAmount = tvSpentAmount,
                tvProgressPercent = tvProgressPercent,
                progressBudget = progressBudget,
                tvMinLabel = tvMinLabel,
                tvMaxLabel = tvMaxLabel,
                tvProgressStatus = tvProgressStatus,
                tvFilterError = tvFilterError
            )
        }
    }

    // ── CHART GENERATION ──────────────────────────────────────────────────────

    private fun generateChart(
        userId: Int,
        fromDate: String,
        toDate: String,
        expenseRepository: ExpenseRepository,
        categoryRepository: CategoryRepository,
        budgetGoalRepository: BudgetGoalRepository,
        barChart: BarChart,
        cardChart: CardView,
        cardGoalInfo: CardView,
        cardBudgetProgress: CardView,
        tvNoData: TextView,
        tvChartSubtitle: TextView,
        tvGoalMin: TextView,
        tvGoalMax: TextView,
        tvGoalStatus: TextView,
        tvSpentAmount: TextView,
        tvProgressPercent: TextView,
        progressBudget: ProgressBar,
        tvMinLabel: TextView,
        tvMaxLabel: TextView,
        tvProgressStatus: TextView,
        tvFilterError: TextView
    ) {
        lifecycleScope.launch {

            // Load category totals for the selected range
            val categoryTotals = withContext(Dispatchers.IO) {
                expenseRepository.getCategoryTotalsInRange(userId, fromDate, toDate)
            }

            // Load categories for name lookup
            val categories = withContext(Dispatchers.IO) {
                categoryRepository.getCategoriesOnce(userId)
            }
            val categoryMap = categories.associate { it.id to it.name }

            // Load budget goals
            val budgetGoal = withContext(Dispatchers.IO) {
                budgetGoalRepository.getBudgetGoal(userId)
            }

            // Load this month's total for the progress bar
            val monthPrefix = fromDate.substring(0, 7) // "yyyy-MM"
            val monthTotal = withContext(Dispatchers.IO) {
                expenseRepository.getTotalForMonth(userId, monthPrefix)
            }

            // ── NO DATA STATE ─────────────────────────────────────────────────
            if (categoryTotals.isEmpty()) {
                cardChart.visibility = View.GONE
                cardGoalInfo.visibility = View.GONE
                cardBudgetProgress.visibility = View.GONE
                tvNoData.visibility = View.VISIBLE
                return@launch
            }

            tvNoData.visibility = View.GONE

            // ── GOAL INFO CARD ────────────────────────────────────────────────
            if (budgetGoal != null) {
                cardGoalInfo.visibility = View.VISIBLE
                tvGoalMin.text = "Min: R %.2f".format(budgetGoal.minGoal)
                tvGoalMax.text = "Max: R %.2f".format(budgetGoal.maxGoal)

                tvGoalStatus.text = when {
                    monthTotal < budgetGoal.minGoal ->
                        "⚠ Under minimum goal this month"
                    monthTotal <= budgetGoal.maxGoal ->
                        "✓ On track this month"
                    else ->
                        "⚠ Over maximum goal this month"
                }
            } else {
                cardGoalInfo.visibility = View.GONE
            }

            // ── BUILD BAR CHART ENTRIES ───────────────────────────────────────
            val entries = mutableListOf<BarEntry>()
            val labels = mutableListOf<String>()

            categoryTotals.forEachIndexed { index, categoryTotal ->
                entries.add(BarEntry(index.toFloat(), categoryTotal.total.toFloat()))
                val name = categoryMap[categoryTotal.category_id] ?: "Other"
                // Truncate long names so they fit on the X axis
                labels.add(if (name.length > 8) name.substring(0, 7) + "…" else name)
            }

            // ── BAR DATASET ───────────────────────────────────────────────────
            val barColors = listOf(
                Color.parseColor("#FF6200EE"), // purple
                Color.parseColor("#FF018786"), // teal
                Color.parseColor("#FF3700B3"), // deep purple
                Color.parseColor("#FF03DAC5"), // teal light
                Color.parseColor("#FF270085"), // darkest purple
                Color.parseColor("#FFBB86FC")  // light purple
            )

            val dataSet = BarDataSet(entries, "Spending per Category").apply {
                colors = List(entries.size) { i -> barColors[i % barColors.size] }
                valueTextColor = Color.BLACK
                valueTextSize = 11f
                setDrawValues(true)
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "R %.0f".format(value)
                    }
                }
            }

            val barData = BarData(dataSet).apply {
                barWidth = 0.6f
            }

            // ── CONFIGURE CHART ───────────────────────────────────────────────
            barChart.apply {
                data = barData
                description.isEnabled = false
                legend.isEnabled = false
                setFitBars(true)
                setDrawGridBackground(false)
                setDrawBorders(false)
                animateY(800)
                setExtraOffsets(8f, 16f, 8f, 8f)

                // X Axis — category names
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(labels)
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                    textColor = Color.DKGRAY
                    textSize = 11f
                    labelRotationAngle = -20f
                }

                // Left Y axis — amounts
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = Color.parseColor("#22000000")
                    textColor = Color.DKGRAY
                    axisMinimum = 0f

                    // ── GOAL LIMIT LINES ──────────────────────────────────────
                    // These are the min and max lines overlaid on the chart
                    removeAllLimitLines()

                    if (budgetGoal != null) {
                        val minLine = LimitLine(
                            budgetGoal.minGoal.toFloat(),
                            "Min Goal"
                        ).apply {
                            lineWidth = 2f
                            lineColor = Color.parseColor("#FF03DAC5") // teal
                            textColor = Color.parseColor("#FF018786")
                            textSize = 10f
                            enableDashedLine(10f, 5f, 0f)
                            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                        }

                        val maxLine = LimitLine(
                            budgetGoal.maxGoal.toFloat(),
                            "Max Goal"
                        ).apply {
                            lineWidth = 2f
                            lineColor = Color.parseColor("#FFB00020") // red
                            textColor = Color.parseColor("#FFB00020")
                            textSize = 10f
                            enableDashedLine(10f, 5f, 0f)
                            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                        }

                        addLimitLine(minLine)
                        addLimitLine(maxLine)
                        setDrawLimitLinesBehindData(true)
                    }
                }

                // Disable right Y axis
                axisRight.isEnabled = false

                invalidate() // refresh chart
            }

            // Show chart subtitle
            tvChartSubtitle.text = "$fromDate  →  $toDate"
            cardChart.visibility = View.VISIBLE

            // ── BUDGET PROGRESS CARD ──────────────────────────────────────────
            if (budgetGoal != null) {
                cardBudgetProgress.visibility = View.VISIBLE

                val maxGoal = budgetGoal.maxGoal
                val minGoal = budgetGoal.minGoal

                tvSpentAmount.text = "Spent this month: R %.2f".format(monthTotal)
                tvMinLabel.text = "Min: R %.2f".format(minGoal)
                tvMaxLabel.text = "Max: R %.2f".format(maxGoal)

                // Progress bar — percentage of max goal spent
                val progressPercent = if (maxGoal > 0) {
                    ((monthTotal / maxGoal) * 100).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                progressBudget.progress = progressPercent
                tvProgressPercent.text = "$progressPercent%"

                // Change progress bar colour based on status
                when {
                    monthTotal < minGoal -> {
                        progressBudget.progressTintList =
                            ContextCompat.getColorStateList(
                                this@SpendingChartActivity,
                                android.R.color.holo_orange_light
                            )
                        tvProgressStatus.text = "⚠ Under minimum goal — keep spending within your plan!"
                        tvProgressStatus.setTextColor(
                            ContextCompat.getColor(this@SpendingChartActivity, R.color.teal_700)
                        )
                    }
                    monthTotal <= maxGoal -> {
                        progressBudget.progressTintList =
                            ContextCompat.getColorStateList(
                                this@SpendingChartActivity,
                                android.R.color.holo_green_light
                            )
                        tvProgressStatus.text = "✓ On track — you are within your budget range!"
                        tvProgressStatus.setTextColor(
                            ContextCompat.getColor(this@SpendingChartActivity, R.color.success_green)
                        )
                    }
                    else -> {
                        progressBudget.progressTintList =
                            ContextCompat.getColorStateList(
                                this@SpendingChartActivity,
                                android.R.color.holo_red_light
                            )
                        tvProgressStatus.text = "⚠ Over maximum goal — review your spending!"
                        tvProgressStatus.setTextColor(
                            ContextCompat.getColor(this@SpendingChartActivity, R.color.error_red)
                        )
                    }
                }
            } else {
                cardBudgetProgress.visibility = View.GONE
            }
        }
    }
}