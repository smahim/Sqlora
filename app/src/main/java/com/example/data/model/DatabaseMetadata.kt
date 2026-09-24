package com.example.data.model

data class DatabaseMetadata(
    val summary: DatabaseSummary,
    val tables: List<TableMetadata>,
    val views: List<ViewMetadata>,
    val triggers: List<TriggerMetadata>
)

data class DatabaseSummary(
    val sqliteVersion: String,
    val pageSizeBytes: Long,
    val pageCount: Long,
    val fileSizeBytes: Long,
    val encoding: String,
    val journalMode: String,
    val autoVacuum: String,
    val userVersion: Int,
    val tableCount: Int,
    val viewCount: Int,
    val indexCount: Int,
    val triggerCount: Int
)

data class TableMetadata(
    val name: String,
    val rowCount: Long,
    val sql: String,
    val columns: List<ColumnInfo>,
    val primaryKeys: List<String>,
    val foreignKeys: List<ForeignKeyInfo>,
    val indexes: List<IndexInfo>
)

data class ViewMetadata(
    val name: String,
    val sql: String,
    val columns: List<ColumnInfo>
)

data class TriggerMetadata(
    val name: String,
    val targetTable: String,
    val event: String, // e.g., INSERT, UPDATE, DELETE
    val timing: String, // e.g., BEFORE, AFTER, INSTEAD OF
    val sql: String
)

data class ForeignKeyInfo(
    val id: Int,
    val seq: Int,
    val targetTable: String,
    val fromColumn: String,
    val toColumn: String,
    val onUpdate: String,
    val onDelete: String
)

data class IndexInfo(
    val name: String,
    val isUnique: Boolean,
    val origin: String, // 'c' = create index, 'u' = unique constraint, 'pk' = primary key
    val columns: List<String>
)
