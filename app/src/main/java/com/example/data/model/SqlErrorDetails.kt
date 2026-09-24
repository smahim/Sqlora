package com.example.data.model

data class SqlErrorDetails(
    val title: String,
    val userExplanation: String,
    val technicalMessage: String,
    val suggestion: String? = null
)
