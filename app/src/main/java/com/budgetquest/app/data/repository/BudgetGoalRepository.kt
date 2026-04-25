package com.budgetquest.app.data.repository

import com.budgetquest.app.data.db.dao.BudgetGoalDao
import com.budgetquest.app.data.db.entity.BudgetGoal
import kotlinx.coroutines.flow.Flow

class BudgetGoalRepository(private val budgetGoalDao: BudgetGoalDao) {

    suspend fun getBudgetGoal(userId: Int): BudgetGoal? {
        return budgetGoalDao.getBudgetGoalByUser(userId)
    }

    fun getBudgetGoalFlow(userId: Int): Flow<BudgetGoal?> {
        return budgetGoalDao.getBudgetGoalFlow(userId)
    }

    suspend fun saveOrUpdateGoal(
        userId: Int,
        minGoal: Double,
        maxGoal: Double
    ): Result<Unit> {
        return try {
            if (minGoal < 0) return Result.failure(Exception("Minimum goal cannot be negative."))
            if (maxGoal < 0) return Result.failure(Exception("Maximum goal cannot be negative."))
            if (maxGoal < minGoal) return Result.failure(
                Exception("Maximum goal must be greater than or equal to minimum goal.")
            )

            val existing = budgetGoalDao.getBudgetGoalByUser(userId)
            if (existing != null) {
                budgetGoalDao.updateBudgetGoal(
                    existing.copy(minGoal = minGoal, maxGoal = maxGoal)
                )
            } else {
                budgetGoalDao.insertOrReplace(
                    BudgetGoal(userId = userId, minGoal = minGoal, maxGoal = maxGoal)
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save budget goal: ${e.message}"))
        }
    }
}