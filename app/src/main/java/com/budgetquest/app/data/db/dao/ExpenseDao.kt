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

    @Query("SELECT * FROM expenses WHERE user_id = :userId ORDER BY date DESC, time DESC")
    fun getExpensesByUser(userId: Int): Flow<List<Expense>>

    @Query("""
        SELECT * FROM expenses 
        WHERE user_id = :userId 
        AND date >= :fromDate 
        AND date <= :toDate 
        ORDER BY date DESC, time DESC
    """)
    fun getExpensesByDateRange(
        userId: Int,
        fromDate: String,
        toDate: String
    ): Flow<List<Expense>>

    @Query("""
        SELECT category_id, SUM(amount) as total 
        FROM expenses 
        WHERE user_id = :userId AND category_id IS NOT NULL
        GROUP BY category_id
    """)
    suspend fun getCategoryTotals(userId: Int): List<CategoryTotal>

    // For chart feature — totals per category within a date range
    @Query("""
        SELECT category_id, SUM(amount) as total 
        FROM expenses 
        WHERE user_id = :userId 
        AND category_id IS NOT NULL
        AND date >= :fromDate AND date <= :toDate
        GROUP BY category_id
    """)
    suspend fun getCategoryTotalsInRange(
        userId: Int,
        fromDate: String,
        toDate: String
    ): List<CategoryTotal>

    @Query("""
        SELECT SUM(amount) 
        FROM expenses 
        WHERE user_id = :userId 
        AND date LIKE :monthPrefix || '%'
    """)
    suspend fun getTotalForMonth(userId: Int, monthPrefix: String): Double?

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Int): Expense?

    // For achievements — total count of expenses for a user
    @Query("SELECT COUNT(*) FROM expenses WHERE user_id = :userId")
    suspend fun getExpenseCount(userId: Int): Int

    // For admin — ALL expenses across ALL users
    @Query("SELECT * FROM expenses ORDER BY date DESC, time DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE user_id = :userId ORDER BY date DESC, time DESC")
    suspend fun getExpensesByUserOnce(userId: Int): List<Expense>
}

data class CategoryTotal(
    val category_id: Int,
    val total: Double
)