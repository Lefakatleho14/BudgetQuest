package com.budgetquest.app.data.repository

import com.budgetquest.app.data.db.dao.AchievementDao
import com.budgetquest.app.data.db.dao.UserDao
import com.budgetquest.app.data.db.entity.Achievement
import com.budgetquest.app.data.db.entity.UserAchievement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Result returned when checking for new achievements — used to show popups
data class UnlockedAchievementResult(
    val achievement: Achievement,
    val newLevel: Int?,   // non-null if this unlock caused a level-up
)

class AchievementRepository(
    private val achievementDao: AchievementDao,
    private val userDao: UserDao
) {

    suspend fun getAllAchievements(): List<Achievement> {
        return achievementDao.getAllAchievements()
    }

    suspend fun getUnlockedIds(userId: Int): List<String> {
        return achievementDao.getUnlockedAchievementIds(userId)
    }

    suspend fun getUnlockedCount(userId: Int): Int {
        return achievementDao.getUnlockedCount(userId)
    }

    // Award XP to a user and recalculate their level
    // Level formula: every 100 XP = 1 level (level = (xp / 100) + 1)
    suspend fun awardXp(userId: Int, xpAmount: Int): Int? {
        val user = userDao.getUserById(userId) ?: return null
        val newXp = user.xp + xpAmount
        val oldLevel = user.level
        val newLevel = (newXp / 100) + 1

        userDao.updateUser(user.copy(xp = newXp, level = newLevel))

        return if (newLevel > oldLevel) newLevel else null
    }

    // Attempts to unlock an achievement if not already unlocked.
    // Returns the unlocked achievement (with level-up info) or null if already unlocked / not found.
    suspend fun tryUnlock(userId: Int, achievementId: String): UnlockedAchievementResult? {
        val alreadyUnlocked = achievementDao.getUnlockedAchievementIds(userId)
        if (achievementId in alreadyUnlocked) return null

        val achievement = achievementDao.getAllAchievements()
            .find { it.id == achievementId } ?: return null

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val inserted = achievementDao.unlockAchievement(
            UserAchievement(
                userId = userId,
                achievementId = achievementId,
                unlockedAt = timestamp
            )
        )

        // If insert was ignored (race condition / already exists), don't award XP again
        if (inserted == -1L) return null

        val newLevel = awardXp(userId, achievement.xpReward)

        return UnlockedAchievementResult(achievement, newLevel)
    }

    // ── ACHIEVEMENT CHECK FUNCTIONS ────────────────────────────────────────
    // Called after relevant actions (add expense, add category, set goal, etc.)
    // Returns a list of newly unlocked achievements (usually 0 or 1)

    suspend fun checkExpenseAchievements(userId: Int, expenseCount: Int): List<UnlockedAchievementResult> {
        val results = mutableListOf<UnlockedAchievementResult>()

        if (expenseCount == 1) {
            tryUnlock(userId, "FIRST_EXPENSE")?.let { results.add(it) }
        }
        if (expenseCount == 10) {
            tryUnlock(userId, "TEN_EXPENSES")?.let { results.add(it) }
        }
        if (expenseCount == 50) {
            tryUnlock(userId, "FIFTY_EXPENSES")?.let { results.add(it) }
        }

        return results
    }

    suspend fun checkCategoryAchievement(userId: Int): UnlockedAchievementResult? {
        return tryUnlock(userId, "FIRST_CATEGORY")
    }

    suspend fun checkGoalAchievement(userId: Int): UnlockedAchievementResult? {
        return tryUnlock(userId, "FIRST_GOAL")
    }

    suspend fun checkBudgetWithinRangeAchievement(userId: Int): UnlockedAchievementResult? {
        return tryUnlock(userId, "WITHIN_BUDGET")
    }

    // ── STREAK TRACKING ───────────────────────────────────────────────────
    // Call this once per app session (e.g. Dashboard onCreate)
    // Returns achievements unlocked due to streak milestones
    suspend fun updateDailyStreak(userId: Int): List<UnlockedAchievementResult> {
        val user = userDao.getUserById(userId) ?: return emptyList()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (user.lastActiveDate == today) {
            // Already counted today — no change
            return emptyList()
        }

        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000))

        val newStreak = if (user.lastActiveDate == yesterday) {
            user.currentStreak + 1
        } else {
            1 // streak broken or first time — restart at 1
        }

        val newLongest = maxOf(user.longestStreak, newStreak)

        userDao.updateUser(
            user.copy(
                currentStreak = newStreak,
                longestStreak = newLongest,
                lastActiveDate = today
            )
        )

        val results = mutableListOf<UnlockedAchievementResult>()
        if (newStreak == 3) {
            tryUnlock(userId, "STREAK_3")?.let { results.add(it) }
        }
        if (newStreak == 7) {
            tryUnlock(userId, "STREAK_7")?.let { results.add(it) }
        }

        return results
    }
}