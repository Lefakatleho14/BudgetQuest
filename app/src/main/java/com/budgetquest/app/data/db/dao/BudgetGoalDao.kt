package com.budgetquest.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetquest.app.data.db.entity.BudgetGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(budgetGoal: BudgetGoal): Long

    @Update
    suspend fun updateBudgetGoal(budgetGoal: BudgetGoal)

    @Query("SELECT * FROM budget_goals WHERE user_id = :userId LIMIT 1")
    suspend fun getBudgetGoalByUser(userId: Int): BudgetGoal?

    @Query("SELECT * FROM budget_goals WHERE user_id = :userId LIMIT 1")
    fun getBudgetGoalFlow(userId: Int): Flow<BudgetGoal?>
}