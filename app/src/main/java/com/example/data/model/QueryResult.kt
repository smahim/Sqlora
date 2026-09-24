package com.example.data.model

data class QueryResult(
    val columns: List<String> = emptyList(),
    val rows: List<List<String?>> = emptyList(),
    val executionTimeMs: Long = 0,
    val affectedRows: Int = 0,
    val isSelect: Boolean = true,
    val errorMessage: String? = null,
    val errorDetails: SqlErrorDetails? = null,
    val statementCount: Int = 1
) {
    val isSuccess: Boolean
        get() = errorMessage == null && errorDetails == null
}
