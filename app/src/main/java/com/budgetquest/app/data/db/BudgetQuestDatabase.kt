package com.budgetquest.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budgetquest.app.data.db.dao.AchievementDao
import com.budgetquest.app.data.db.dao.BudgetGoalDao
import com.budgetquest.app.data.db.dao.CategoryDao
import com.budgetquest.app.data.db.dao.ExpenseDao
import com.budgetquest.app.data.db.dao.UserDao
import com.budgetquest.app.data.db.entity.Achievement
import com.budgetquest.app.data.db.entity.BudgetGoal
import com.budgetquest.app.data.db.entity.Category
import com.budgetquest.app.data.db.entity.Expense
import com.budgetquest.app.data.db.entity.User
import com.budgetquest.app.data.db.entity.UserAchievement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Category::class,
        Expense::class,
        BudgetGoal::class,
        Achievement::class,
        UserAchievement::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BudgetQuestDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetGoalDao(): BudgetGoalDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: BudgetQuestDatabase? = null

        fun getDatabase(context: Context): BudgetQuestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BudgetQuestDatabase::class.java,
                    "budget_quest_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed the achievement catalog when DB is first created
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.achievementDao().insertAchievements(
                                        AchievementCatalog.ALL
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Static catalog of all achievements available in the app
object AchievementCatalog {
    val ALL = listOf(
        Achievement(
            id = "FIRST_EXPENSE",
            title = "First Steps",
            description = "Log your very first expense",
            iconName = "ic_badge_first",
            xpReward = 10
        ),
        Achievement(
            id = "TEN_EXPENSES",
            title = "Getting Serious",
            description = "Log 10 expenses",
            iconName = "ic_badge_ten",
            xpReward = 25
        ),
        Achievement(
            id = "FIFTY_EXPENSES",
            title = "Budget Master",
            description = "Log 50 expenses",
            iconName = "ic_badge_fifty",
            xpReward = 100
        ),
        Achievement(
            id = "WITHIN_BUDGET",
            title = "On Target",
            description = "Stay within your budget goals for a month",
            iconName = "ic_badge_target",
            xpReward = 50
        ),
        Achievement(
            id = "STREAK_3",
            title = "Building Habits",
            description = "Use the app for 3 days in a row",
            iconName = "ic_badge_streak3",
            xpReward = 15
        ),
        Achievement(
            id = "STREAK_7",
            title = "Week Warrior",
            description = "Use the app for 7 days in a row",
            iconName = "ic_badge_streak7",
            xpReward = 50
        ),
        Achievement(
            id = "FIRST_CATEGORY",
            title = "Organiser",
            description = "Create your first category",
            iconName = "ic_badge_organiser",
            xpReward = 5
        ),
        Achievement(
            id = "FIRST_GOAL",
            title = "Goal Setter",
            description = "Set your first budget goal",
            iconName = "ic_badge_goal",
            xpReward = 10
        )
    )
}