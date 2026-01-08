package com.mintly.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val description: String,
    val categoryId: String?,
    val timestamp: Long = System.currentTimeMillis()
)
