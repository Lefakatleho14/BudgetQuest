package com.budgetquest.app.ui.category

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.budgetquest.app.R
import com.budgetquest.app.data.db.BudgetQuestDatabase
import com.budgetquest.app.data.db.entity.Category
import com.budgetquest.app.data.repository.CategoryRepository
import com.budgetquest.app.utils.SessionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        // Dependencies
        val db = BudgetQuestDatabase.getDatabase(this)
        val categoryRepository = CategoryRepository(db.categoryDao())
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        // Views
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val rvCategories = findViewById<RecyclerView>(R.id.rvCategories)
        val fabAddCategory = findViewById<FloatingActionButton>(R.id.fabAddCategory)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        // Toolbar back navigation
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // RecyclerView setup
        val adapter = CategoryAdapter(
            categories = emptyList(),
            onEditClick = { category -> showEditDialog(category, categoryRepository) },
            onDeleteClick = { category -> showDeleteDialog(category, categoryRepository) }
        )
        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = adapter

        // Observe categories with Flow
        lifecycleScope.launch {
            categoryRepository.getCategoriesForUser(userId).collect { categories ->
                adapter.updateList(categories)
                if (categories.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvCategories.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvCategories.visibility = View.VISIBLE
                }
            }
        }

        // FAB — show Add dialog
        fabAddCategory.setOnClickListener {
            showAddDialog(userId, categoryRepository)
        }
    }

    // ─── ADD DIALOG ───────────────────────────────────────────────────────────

    private fun showAddDialog(userId: Int, categoryRepository: CategoryRepository) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_category, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etCategoryName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val tvDialogError = dialogView.findViewById<TextView>(R.id.tvDialogError)

        tvDialogTitle.text = "Add Category"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save", null) // set null to override below
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val saveButton: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val name = etCategoryName.text.toString().trim()

                if (name.isEmpty()) {
                    tvDialogError.text = "Category name cannot be empty."
                    tvDialogError.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        categoryRepository.addCategory(userId, name)
                    }
                    result.fold(
                        onSuccess = { dialog.dismiss() },
                        onFailure = { e ->
                            tvDialogError.text = e.message ?: "Failed to add category."
                            tvDialogError.visibility = View.VISIBLE
                        }
                    )
                }
            }
        }

        dialog.show()
    }

    // ─── EDIT DIALOG ──────────────────────────────────────────────────────────

    private fun showEditDialog(category: Category, categoryRepository: CategoryRepository) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_category, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etCategoryName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val tvDialogError = dialogView.findViewById<TextView>(R.id.tvDialogError)

        tvDialogTitle.text = "Edit Category"
        etCategoryName.setText(category.name)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val saveButton: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val newName = etCategoryName.text.toString().trim()

                if (newName.isEmpty()) {
                    tvDialogError.text = "Category name cannot be empty."
                    tvDialogError.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                val updatedCategory = category.copy(name = newName)

                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        categoryRepository.updateCategory(updatedCategory)
                    }
                    result.fold(
                        onSuccess = { dialog.dismiss() },
                        onFailure = { e ->
                            tvDialogError.text = e.message ?: "Failed to update category."
                            tvDialogError.visibility = View.VISIBLE
                        }
                    )
                }
            }
        }

        dialog.show()
    }

    // ─── DELETE DIALOG ────────────────────────────────────────────────────────

    private fun showDeleteDialog(category: Category, categoryRepository: CategoryRepository) {
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete \"${category.name}\"?\n\nExpenses in this category will not be deleted, but will become uncategorised.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        categoryRepository.deleteCategory(category)
                    }
                    result.fold(
                        onSuccess = {
                            // Flow auto-refreshes the list
                        },
                        onFailure = { e ->
                            AlertDialog.Builder(this@CategoryActivity)
                                .setTitle("Error")
                                .setMessage(e.message ?: "Failed to delete category.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}