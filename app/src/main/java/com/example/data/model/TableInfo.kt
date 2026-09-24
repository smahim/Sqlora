package com.example.data.model

data class TableInfo(
    val name: String,
    val type: String, // TABLE or VIEW
    val rowCount: Long = 0,
    val columnCount: Int = 0,
    val sql: String = ""
)
