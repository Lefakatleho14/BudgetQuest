package com.budgetquest.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetquest.app.data.db.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // All expenses for a user, newest date first
    @Query("SELECT * FROM expenses WHERE user_id = :userId ORDER BY date DESC")
    fun getExpensesByUser(userId: Int): Flow<List<Expense>>

    // Filter by date range (inclusive), format: "yyyy-MM-dd"
    @Query("""
        SELECT * FROM expenses 
        WHERE user_id = :userId 
        AND date >= :fromDate 
        AND date <= :toDate 
        ORDER BY date DESC
    """)
    fun getExpensesByDateRange(
        userId: Int,
        fromDate: String,
        toDate: String
    ): Flow<List<Expense>>

    // Total spent per category for a given user (for category totals screen)
    @Query("""
        SELECT category_id, SUM(amount) as total 
        FROM expenses 
        WHERE user_id = :userId AND category_id IS NOT NULL
        GROUP BY category_id
    """)
    suspend fun getCategoryTotals(userId: Int): List<CategoryTotal>

    // Total spent in a specific month, e.g. month = "2024-06"
    @Query("""
        SELECT SUM(amount) 
        FROM expenses 
        WHERE user_id = :userId 
        AND date LIKE :monthPrefix || '%'
    """)
    suspend fun getTotalForMonth(userId: Int, monthPrefix: String): Double?

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Int): Expense?
}

// Lightweight data class used only for the category totals query result
data class CategoryTotal(
    val category_id: Int,
    val total: Double
)