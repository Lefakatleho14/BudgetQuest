package com.budgetquest.app.ui.expense

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.budgetquest.app.R
import com.budgetquest.app.data.db.BudgetQuestDatabase
import com.budgetquest.app.data.db.entity.Category
import com.budgetquest.app.data.db.entity.Expense
import com.budgetquest.app.data.repository.AchievementRepository
import com.budgetquest.app.data.repository.CategoryRepository
import com.budgetquest.app.data.repository.ExpenseRepository
import com.budgetquest.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

    // Holds the URI string of the selected image
    private var selectedImageUri: String? = null

    // Holds categories loaded from DB — index matches RadioButton ID offset
    private var categoryList: List<Category> = emptyList()

    // Image picker launcher
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri.toString()
                val ivImagePreview = findViewById<ImageView>(R.id.ivImagePreview)
                ivImagePreview.setImageURI(uri)
                ivImagePreview.visibility = View.VISIBLE
            }
        }

    // Permission launcher (for Android 13+)
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                imagePickerLauncher.launch("image/*")
            } else {
                showError("Permission denied. Cannot access gallery.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        // Dependencies
        val db = BudgetQuestDatabase.getDatabase(this)
        val expenseRepository = ExpenseRepository(db.expenseDao())
        val categoryRepository = CategoryRepository(db.categoryDao())
        val achievementRepository = AchievementRepository(db.achievementDao(), db.userDao())
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        // Views
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnPickDate = findViewById<Button>(R.id.btnPickDate)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val radioGroupCategory = findViewById<RadioGroup>(R.id.radioGroupCategory)
        val tvCategoryError = findViewById<TextView>(R.id.tvCategoryError)
        val btnAttachImage = findViewById<Button>(R.id.btnAttachImage)
        val ivImagePreview = findViewById<ImageView>(R.id.ivImagePreview)
        val tvError = findViewById<TextView>(R.id.tvError)
        val btnSaveExpense = findViewById<Button>(R.id.btnSaveExpense)

        // Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Track selected date — defaults to today
        var selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        btnPickDate.text = selectedDate
        btnPickDate.setTextColor(ContextCompat.getColor(this, R.color.black))

        // ── DATE PICKER ──────────────────────────────────────────────────────
        btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
                    btnPickDate.text = selectedDate
                    btnPickDate.setTextColor(ContextCompat.getColor(this, R.color.black))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // ── LOAD CATEGORIES INTO RADIO GROUP ──────────────────────────────────
        lifecycleScope.launch {
            categoryList = withContext(Dispatchers.IO) {
                categoryRepository.getCategoriesOnce(userId)
            }

            if (categoryList.isEmpty()) {
                // No categories at all — show a message and disable category selection
                tvCategoryError.text = "No categories found. Add one in Categories first, " +
                        "or save this expense as Uncategorised."
                tvCategoryError.visibility = View.VISIBLE
            } else {
                // Dynamically create a RadioButton for each category
                categoryList.forEachIndexed { index, category ->
                    val radioButton = RadioButton(this@AddExpenseActivity)
                    radioButton.id = index // RadioButton ID = position in categoryList
                    radioButton.text = category.name
                    radioButton.textSize = 15f
                    radioButton.setPadding(16, 14, 16, 14)
                    radioButton.setTextColor(
                        ContextCompat.getColor(this@AddExpenseActivity, R.color.black)
                    )
                    radioGroupCategory.addView(radioButton)
                }

                // Add an "Uncategorised" option at the end
                val noneRadioButton = RadioButton(this@AddExpenseActivity)
                noneRadioButton.id = categoryList.size // one past the last category index
                noneRadioButton.text = "Uncategorised"
                noneRadioButton.textSize = 15f
                noneRadioButton.setPadding(16, 14, 16, 14)
                noneRadioButton.setTextColor(
                    ContextCompat.getColor(this@AddExpenseActivity, R.color.text_secondary)
                )
                radioGroupCategory.addView(noneRadioButton)

                // Default selection — first category pre-selected so the
                // user always has a sensible default (addresses lecturer
                // feedback about users not knowing which category to pick)
                radioGroupCategory.check(0)
            }
        }

        // ── IMAGE PICKER ──────────────────────────────────────────────────────
        btnAttachImage.setOnClickListener {
            openImagePicker()
        }

        // ── SAVE EXPENSE ──────────────────────────────────────────────────────
        btnSaveExpense.setOnClickListener {
            val amountText = etAmount.text.toString().trim()
            val description = etDescription.text.toString().trim()

            // Validation
            if (amountText.isEmpty()) {
                showError("Amount is required.")
                return@setOnClickListener
            }
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                showError("Please enter a valid amount greater than zero.")
                return@setOnClickListener
            }
            if (description.isEmpty()) {
                showError("Description is required.")
                return@setOnClickListener
            }

            // Resolve selected category from RadioGroup
            val checkedId = radioGroupCategory.checkedRadioButtonId
            val resolvedCategoryId: Int? = if (categoryList.isNotEmpty() && checkedId != -1) {
                if (checkedId < categoryList.size) {
                    categoryList[checkedId].id
                } else {
                    null // "Uncategorised" option was selected
                }
            } else {
                null
            }

            // Auto-generate the time — current time at the moment of saving
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            // Build expense object
            val expense = Expense(
                userId = userId,
                amount = amount,
                date = selectedDate,
                time = currentTime,
                description = description,
                categoryId = resolvedCategoryId,
                imageUri = selectedImageUri
            )

            btnSaveExpense.isEnabled = false
            tvError.visibility = View.GONE

            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    expenseRepository.addExpense(expense)
                }
                result.fold(
                    onSuccess = {
                        // Check for expense-count achievements (First Steps, 10, 50)
                        val newCount = withContext(Dispatchers.IO) {
                            expenseRepository.getExpenseCount(userId)
                        }
                        val unlocked = withContext(Dispatchers.IO) {
                            achievementRepository.checkExpenseAchievements(userId, newCount)
                        }

                        if (unlocked.isNotEmpty()) {
                            val resultIntent = Intent()
                            resultIntent.putExtra(
                                "unlocked_achievement_title",
                                unlocked.first().achievement.title
                            )
                            resultIntent.putExtra(
                                "unlocked_achievement_desc",
                                unlocked.first().achievement.description
                            )
                            setResult(RESULT_OK, resultIntent)
                        } else {
                            setResult(RESULT_OK)
                        }
                        finish()
                    },
                    onFailure = { e ->
                        showError(e.message ?: "Failed to save expense.")
                        btnSaveExpense.isEnabled = true
                    }
                )
            }
        }
    }

    // ── IMAGE PICKER HELPER ───────────────────────────────────────────────────

    private fun openImagePicker() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    imagePickerLauncher.launch("image/*")
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    imagePickerLauncher.launch("image/*")
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            else -> {
                imagePickerLauncher.launch("image/*")
            }
        }
    }

    private fun showError(message: String) {
        val tvError = findViewById<TextView>(R.id.tvError)
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
}