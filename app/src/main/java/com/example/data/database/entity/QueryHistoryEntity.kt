package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "query_history")
data class QueryHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val databaseId: Long,
    val sqlQuery: String,
    val executedAt: Long = System.currentTimeMillis(),
    val executionTimeMs: Long = 0,
    val isSuccess: Boolean = true,
    val rowsAffected: Int = 0,
    val errorMessage: String? = null
)
