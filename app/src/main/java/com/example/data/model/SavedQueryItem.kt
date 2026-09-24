package com.example.data.model

data class SavedQueryItem(
    val id: Long,
    val title: String,
    val sqlQuery: String,
    val databaseId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
