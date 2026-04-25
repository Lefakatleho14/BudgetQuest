package com.budgetquest.app.data.repository

import com.budgetquest.app.data.db.dao.CategoryTotal
import com.budgetquest.app.data.db.dao.ExpenseDao
import com.budgetquest.app.data.db.entity.Expense
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun getExpensesForUser(userId: Int): Flow<List<Expense>> {
        return expenseDao.getExpensesByUser(userId)
    }

    fun getExpensesByDateRange(
        userId: Int,
        fromDate: String,
        toDate: String
    ): Flow<List<Expense>> {
        return expenseDao.getExpensesByDateRange(userId, fromDate, toDate)
    }

    suspend fun addExpense(expense: Expense): Result<Long> {
        return try {
            validateExpense(expense)?.let { error ->
                return Result.failure(Exception(error))
            }
            val id = expenseDao.insertExpense(expense)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save expense: ${e.message}"))
        }
    }

    suspend fun deleteExpense(expense: Expense): Result<Unit> {
        return try {
            expenseDao.deleteExpense(expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete expense: ${e.message}"))
        }
    }

    suspend fun getCategoryTotals(userId: Int): List<CategoryTotal> {
        return expenseDao.getCategoryTotals(userId)
    }

    suspend fun getTotalForMonth(userId: Int, monthPrefix: String): Double {
        return expenseDao.getTotalForMonth(userId, monthPrefix) ?: 0.0
    }

    suspend fun getExpenseById(id: Int): Expense? {
        return expenseDao.getExpenseById(id)
    }

    // Returns an error string if invalid, null if valid
    private fun validateExpense(expense: Expense): String? {
        if (expense.amount <= 0) return "Amount must be greater than zero."
        if (expense.date.isBlank()) return "Date is required."
        if (expense.startTime.isBlank()) return "Start time is required."
        if (expense.endTime.isBlank()) return "End time is required."
        if (expense.description.isBlank()) return "Description is required."
        return null
    }
}