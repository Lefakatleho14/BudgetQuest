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

    // Stored as "yyyy-MM-dd"
    @ColumnInfo(name = "date")
    val date: String,

    // Stored as "HH:mm"
    @ColumnInfo(name = "start_time")
    val startTime: String,

    // Stored as "HH:mm"
    @ColumnInfo(name = "end_time")
    val endTime: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "category_id")
    val categoryId: Int?,

    // URI string of attached image, nullable
    @ColumnInfo(name = "image_uri")
    val imageUri: String?
)