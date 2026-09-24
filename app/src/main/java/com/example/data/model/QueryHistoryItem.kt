package com.example.data.model

data class QueryHistoryItem(
    val id: Long,
    val databaseId: Long,
    val sqlQuery: String,
    val executedAt: Long,
    val executionTimeMs: Long,
    val isSuccess: Boolean,
    val rowsAffected: Int,
    val errorMessage: String? = null
)
