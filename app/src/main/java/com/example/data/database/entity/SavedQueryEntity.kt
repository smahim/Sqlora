package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_queries")
data class SavedQueryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sqlQuery: String,
    val databaseId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
