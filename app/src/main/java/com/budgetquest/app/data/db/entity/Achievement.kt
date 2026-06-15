package com.budgetquest.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Static catalog of all possible achievements/badges in the app
@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,           // unique code, e.g. "FIRST_EXPENSE"

    @ColumnInfo(name = "title")
    val title: String,        // "First Steps"

    @ColumnInfo(name = "description")
    val description: String,  // "Log your first expense"

    @ColumnInfo(name = "icon_name")
    val iconName: String,      // maps to a drawable resource name

    @ColumnInfo(name = "xp_reward")
    val xpReward: Int          // XP granted when unlocked
)