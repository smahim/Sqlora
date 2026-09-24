package com.example.data.model

data class ColumnInfo(
    val cid: Int,
    val name: String,
    val type: String,
    val isNotNull: Boolean,
    val defaultValue: String?,
    val isPrimaryKey: Boolean
)
