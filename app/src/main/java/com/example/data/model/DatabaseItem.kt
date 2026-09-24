package com.example.data.model

data class DatabaseItem(
    val id: Long,
    val name: String,
    val filePath: String,
    val description: String = "",
    val folder: String = "General",
    val tableCount: Int = 0,
    val sizeBytes: Long = 0,
    val isSample: Boolean = false,
    val uriString: String? = null,
    val isSafBacked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
