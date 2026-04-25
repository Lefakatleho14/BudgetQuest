package com.budgetquest.app.data.repository

import com.budgetquest.app.data.db.dao.CategoryDao
import com.budgetquest.app.data.db.entity.Category
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getCategoriesForUser(userId: Int): Flow<List<Category>> {
        return categoryDao.getCategoriesByUser(userId)
    }

    suspend fun getCategoriesOnce(userId: Int): List<Category> {
        return categoryDao.getCategoriesByUserOnce(userId)
    }

    suspend fun addCategory(userId: Int, name: String): Result<Long> {
        return try {
            if (name.isBlank()) {
                return Result.failure(Exception("Category name cannot be empty."))
            }
            val category = Category(userId = userId, name = name.trim())
            val id = categoryDao.insertCategory(category)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to add category: ${e.message}"))
        }
    }

    suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            if (category.name.isBlank()) {
                return Result.failure(Exception("Category name cannot be empty."))
            }
            categoryDao.updateCategory(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update category: ${e.message}"))
        }
    }

    suspend fun deleteCategory(category: Category): Result<Unit> {
        return try {
            categoryDao.deleteCategory(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete category: ${e.message}"))
        }
    }

    suspend fun getCategoryById(id: Int): Category? {
        return categoryDao.getCategoryById(id)
    }
}