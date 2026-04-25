package com.budgetquest.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.budgetquest.app.data.db.dao.BudgetGoalDao
import com.budgetquest.app.data.db.dao.CategoryDao
import com.budgetquest.app.data.db.dao.ExpenseDao
import com.budgetquest.app.data.db.dao.UserDao
import com.budgetquest.app.data.db.entity.BudgetGoal
import com.budgetquest.app.data.db.entity.Category
import com.budgetquest.app.data.db.entity.Expense
import com.budgetquest.app.data.db.entity.User

@Database(
    entities = [
        User::class,
        Category::class,
        Expense::class,
        BudgetGoal::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BudgetQuestDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetGoalDao(): BudgetGoalDao

    companion object {
        // Volatile ensures the INSTANCE is always up-to-date across all threads
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
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}