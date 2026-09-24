package com.example.data.sqlite

import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.database.sqlite.SQLiteOutOfMemoryException
import android.database.sqlite.SQLiteTableLockedException
import com.example.data.model.SqlErrorDetails

object SqlErrorParser {

    fun parse(e: Throwable, sql: String? = null): SqlErrorDetails {
        val technicalMessage = e.message ?: e.toString()
        val lowerMsg = technicalMessage.lowercase()

        return when {
            e is SQLiteConstraintException || lowerMsg.contains("constraint") -> {
                when {
                    lowerMsg.contains("unique") -> SqlErrorDetails(
                        title = "Unique Constraint Violation",
                        userExplanation = "A record with this value already exists. The column has a UNIQUE constraint or is a primary key.",
                        technicalMessage = technicalMessage,
                        suggestion = "Ensure the key or unique value is distinct before inserting or updating."
                    )
                    lowerMsg.contains("not null") -> SqlErrorDetails(
                        title = "Not Null Constraint Violation",
                        userExplanation = "A required field was left empty or NULL.",
                        technicalMessage = technicalMessage,
                        suggestion = "Provide a non-null value for all columns marked with NOT NULL."
                    )
                    lowerMsg.contains("foreign key") -> SqlErrorDetails(
                        title = "Foreign Key Constraint Violation",
                        userExplanation = "The referenced parent record does not exist in the related table.",
                        technicalMessage = technicalMessage,
                        suggestion = "Insert the parent record into the referenced table first, or verify the foreign key value."
                    )
                    lowerMsg.contains("check") -> SqlErrorDetails(
                        title = "Check Constraint Violation",
                        userExplanation = "The inserted value violates the custom CHECK expression defined on this table.",
                        technicalMessage = technicalMessage,
                        suggestion = "Check the table schema DDL to see the allowed range or values."
                    )
                    else -> SqlErrorDetails(
                        title = "Constraint Violation",
                        userExplanation = "The operation violates a schema rule (unique, foreign key, or not null).",
                        technicalMessage = technicalMessage,
                        suggestion = "Review the constraints on this table."
                    )
                }
            }

            e is SQLiteDatabaseLockedException || e is SQLiteTableLockedException || lowerMsg.contains("locked") || lowerMsg.contains("busy") -> {
                SqlErrorDetails(
                    title = "Database Busy or Locked",
                    userExplanation = "Another transaction or process is currently holding a lock on the database.",
                    technicalMessage = technicalMessage,
                    suggestion = "Wait a moment for the pending transaction to complete or commit, then retry."
                )
            }

            e is SQLiteCantOpenDatabaseException || lowerMsg.contains("unable to open") -> {
                SqlErrorDetails(
                    title = "Cannot Open Database",
                    userExplanation = "The SQLite engine could not open the specified file. It might be read-only, moved, or deleted.",
                    technicalMessage = technicalMessage,
                    suggestion = "Check if the file still exists and that the app has permission to read/write it."
                )
            }

            e is SQLiteDatabaseCorruptException || lowerMsg.contains("corrupt") || lowerMsg.contains("malformed") -> {
                SqlErrorDetails(
                    title = "Database Malformed or Corrupt",
                    userExplanation = "The database file contains invalid byte headers or damaged internal pages.",
                    technicalMessage = technicalMessage,
                    suggestion = "Run 'PRAGMA integrity_check;' or restore from a backup file."
                )
            }

            e is SQLiteFullException || lowerMsg.contains("disk full") -> {
                SqlErrorDetails(
                    title = "Disk Full",
                    userExplanation = "Storage space is exhausted. Cannot write more pages.",
                    technicalMessage = technicalMessage,
                    suggestion = "Free up device storage space."
                )
            }

            lowerMsg.contains("no such table") -> {
                val tableName = Regex("no such table:\\s*(\\w+)", RegexOption.IGNORE_CASE)
                    .find(technicalMessage)?.groupValues?.getOrNull(1) ?: "the specified table"
                SqlErrorDetails(
                    title = "Table Not Found",
                    userExplanation = "The table '$tableName' does not exist in this database.",
                    technicalMessage = technicalMessage,
                    suggestion = "Check spelling or use the Tables tab in the schema explorer to verify table names."
                )
            }

            lowerMsg.contains("no such column") -> {
                val colName = Regex("no such column:\\s*([\\w.]+)", RegexOption.IGNORE_CASE)
                    .find(technicalMessage)?.groupValues?.getOrNull(1) ?: "the column"
                SqlErrorDetails(
                    title = "Column Not Found",
                    userExplanation = "Column '$colName' does not exist on the target table.",
                    technicalMessage = technicalMessage,
                    suggestion = "Check column spelling or run PRAGMA table_info('table_name');"
                )
            }

            lowerMsg.contains("syntax error") || lowerMsg.contains("near") -> {
                SqlErrorDetails(
                    title = "SQL Syntax Error",
                    userExplanation = "SQLite parser encountered unexpected tokens or invalid SQL syntax.",
                    technicalMessage = technicalMessage,
                    suggestion = "Check for missing commas, unmatched quotes, or misspelled keywords (e.g., SELECT, FROM, WHERE)."
                )
            }

            else -> {
                SqlErrorDetails(
                    title = "SQLite Execution Error",
                    userExplanation = "An error occurred while executing the statement on the SQLite engine.",
                    technicalMessage = technicalMessage,
                    suggestion = "Check the technical error details below for more information."
                )
            }
        }
    }
}
