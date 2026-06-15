package com.budgetquest.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetquest.app.data.db.entity.Achievement
import com.budgetquest.app.data.db.entity.UserAchievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: Achievement)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievements(): List<Achievement>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlockAchievement(userAchievement: UserAchievement): Long

    @Query("SELECT achievement_id FROM user_achievements WHERE user_id = :userId")
    suspend fun getUnlockedAchievementIds(userId: Int): List<String>

    @Query("SELECT * FROM user_achievements WHERE user_id = :userId")
    fun getUserAchievementsFlow(userId: Int): Flow<List<UserAchievement>>

    @Query("SELECT COUNT(*) FROM user_achievements WHERE user_id = :userId")
    suspend fun getUnlockedCount(userId: Int): Int
}