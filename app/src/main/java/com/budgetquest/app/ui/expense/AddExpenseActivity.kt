package com.budgetquest.app.ui.expense

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
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
import com.budgetquest.app.data.repository.CategoryRepository
import com.budgetquest.app.data.repository.ExpenseRepository
import com.budgetquest.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AddExpenseActivity : AppCompatActivity() {

    private var selectedImageUri: String? = null
    private var categoryList: List<Category> = emptyList()

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri.toString()
                val ivImagePreview = findViewById<ImageView>(R.id.ivImagePreview)
                ivImagePreview.setImageURI(uri)
                ivImagePreview.visibility = View.VISIBLE
            }
        }

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

        val db = BudgetQuestDatabase.getDatabase(this)
        val expenseRepository = ExpenseRepository(db.expenseDao())
        val categoryRepository = CategoryRepository(db.categoryDao())
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnPickDate = findViewById<Button>(R.id.btnPickDate)
        val btnPickStartTime = findViewById<Button>(R.id.btnPickStartTime)
        val btnPickEndTime = findViewById<Button>(R.id.btnPickEndTime)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val btnAttachImage = findViewById<Button>(R.id.btnAttachImage)
        val tvError = findViewById<TextView>(R.id.tvError)
        val btnSaveExpense = findViewById<Button>(R.id.btnSaveExpense)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        var selectedDate = ""
        var selectedStartTime = ""
        var selectedEndTime = ""

        btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
                    btnPickDate.text = selectedDate
                    btnPickDate.setTextColor(ContextCompat.getColor(this, R.color.black))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnPickStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedStartTime = "%02d:%02d".format(hour, minute)
                    btnPickStartTime.text = selectedStartTime
                    btnPickStartTime.setTextColor(ContextCompat.getColor(this, R.color.black))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        btnPickEndTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedEndTime = "%02d:%02d".format(hour, minute)
                    btnPickEndTime.text = selectedEndTime
                    btnPickEndTime.setTextColor(ContextCompat.getColor(this, R.color.black))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        lifecycleScope.launch {
            categoryList = withContext(Dispatchers.IO) {
                categoryRepository.getCategoriesOnce(userId)
            }

            val displayNames = mutableListOf("-- Select Category (Optional) --")
            displayNames.addAll(categoryList.map { it.name })

            val adapter = ArrayAdapter(
                this@AddExpenseActivity,
                android.R.layout.simple_spinner_item,
                displayNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter
        }

        btnAttachImage.setOnClickListener {
            openImagePicker()
        }

        btnSaveExpense.setOnClickListener {
            val amountText = etAmount.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (amountText.isEmpty()) {
                showError("Amount is required.")
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                showError("Invalid amount.")
                return@setOnClickListener
            }

            if (selectedDate.isEmpty() || selectedStartTime.isEmpty() || selectedEndTime.isEmpty()) {
                showError("Date and time required.")
                return@setOnClickListener
            }

            if (description.isEmpty()) {
                showError("Description required.")
                return@setOnClickListener
            }

            val categoryId = if (spinnerCategory.selectedItemPosition > 0) {
                categoryList[spinnerCategory.selectedItemPosition - 1].id
            } else null

            val expense = Expense(
                userId = userId,
                amount = amount,
                date = selectedDate,
                startTime = selectedStartTime,
                endTime = selectedEndTime,
                description = description,
                categoryId = categoryId,
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
                        setResult(RESULT_OK)
                        finish()
                    },
                    onFailure = {
                        showError(it.message ?: "Save failed")
                        btnSaveExpense.isEnabled = true
                    }
                )
            }
        }
    }

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
            else -> imagePickerLauncher.launch("image/*")
        }
    }

    private fun showError(message: String) {
        val tvError = findViewById<TextView>(R.id.tvError)
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
}