package com.budgetquest.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "password")
    val password: String,

    // "admin" or "user" — determines dashboard shown after login
    @ColumnInfo(name = "role", defaultValue = "user")
    val role: String = "user",

    // XP and level for gamification
    @ColumnInfo(name = "xp", defaultValue = "0")
    val xp: Int = 0,

    @ColumnInfo(name = "level", defaultValue = "1")
    val level: Int = 1,

    // Streak tracking
    @ColumnInfo(name = "current_streak", defaultValue = "0")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "longest_streak", defaultValue = "0")
    val longestStreak: Int = 0,

    // Last date the user opened/used the app, format yyyy-MM-dd
    @ColumnInfo(name = "last_active_date")
    val lastActiveDate: String? = null
)