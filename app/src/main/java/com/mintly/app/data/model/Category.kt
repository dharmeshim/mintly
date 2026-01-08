package com.mintly.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val keywords: List<String> = emptyList(),
    val colorDot: Int // ARGB color or custom identifier
)
