package com.budgetquest.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["category_id"])
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "user_id")
    val userId: Int,

    @ColumnInfo(name = "amount")
    val amount: Double,

    // Stored as "yyyy-MM-dd" — user-selected date
    @ColumnInfo(name = "date")
    val date: String,

    // Stored as "HH:mm" — auto-set to current time when saved, NOT user-editable
    @ColumnInfo(name = "time")
    val time: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "category_id")
    val categoryId: Int?,

    @ColumnInfo(name = "image_uri")
    val imageUri: String?
)