package com.example.data.sqlite

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.example.data.model.ColumnInfo
import com.example.data.model.DatabaseMetadata
import com.example.data.model.QueryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class SqliteConnection(val file: File) {

    private val mutex = Mutex()
    private var database: SQLiteDatabase? = null

    val isOpen: Boolean
        get() = database?.isOpen == true

    suspend fun open(readOnly: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isOpen) return@withContext true
            try {
                if (!file.exists()) {
                    file.parentFile?.mkdirs()
                }
                val flags = if (readOnly) {
                    SQLiteDatabase.OPEN_READONLY
                } else {
                    SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
                }
                val db = SQLiteDatabase.openDatabase(file.absolutePath, null, flags)
                // Configure busy timeout so concurrent/rapid accesses do not throw busy lock immediately
                try {
                    db.execSQL("PRAGMA busy_timeout = 5000;")
                } catch (_: Exception) {}
                database = db
                true
            } catch (e: Exception) {
                e.printStackTrace()
                database = null
                false
            }
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (database?.isOpen == true) {
                    database?.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                database = null
            }
        }
    }

    suspend fun reopen(): Boolean {
        close()
        return open()
    }

    private fun getOrOpenDb(): SQLiteDatabase {
        val current = database
        if (current != null && current.isOpen) {
            return current
        }
        val flags = SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
        val db = SQLiteDatabase.openDatabase(file.absolutePath, null, flags)
        try {
            db.execSQL("PRAGMA busy_timeout = 5000;")
        } catch (_: Exception) {}
        database = db
        return db
    }

    suspend fun executeSql(rawSql: String): QueryResult = withContext(Dispatchers.IO) {
        val trimmed = rawSql.trim()
        if (trimmed.isBlank()) {
            return@withContext QueryResult(
                errorMessage = "Empty SQL statement",
                errorDetails = SqlErrorParser.parse(IllegalArgumentException("Empty SQL statement"), rawSql)
            )
        }

        mutex.withLock {
            val startTime = System.currentTimeMillis()
            var db: SQLiteDatabase? = null
            try {
                db = getOrOpenDb()

                // Check if the script contains multiple statements separated by semicolons
                val statements = splitStatements(trimmed)
                if (statements.size > 1) {
                    return@withContext executeMultipleStatements(db, statements, startTime)
                }

                // Single statement execution
                val singleStmt = statements.firstOrNull() ?: trimmed
                if (isReadStatement(singleStmt)) {
                    executeSelect(db, singleStmt, startTime)
                } else {
                    executeDml(db, singleStmt, startTime)
                }
            } catch (e: Throwable) {
                val elapsed = System.currentTimeMillis() - startTime
                val errorDetails = SqlErrorParser.parse(e, trimmed)
                QueryResult(
                    executionTimeMs = elapsed,
                    isSelect = isReadStatement(trimmed),
                    errorMessage = errorDetails.userExplanation,
                    errorDetails = errorDetails
                )
            }
        }
    }

    private fun executeSelect(db: SQLiteDatabase, sql: String, startTime: Long): QueryResult {
        db.rawQuery(sql, null).use { cursor ->
            val columns = cursor.columnNames.toList()
            val rows = mutableListOf<List<String?>>()

            val maxRows = 500 // Generous mobile limit
            var count = 0
            while (cursor.moveToNext() && count < maxRows) {
                val row = mutableListOf<String?>()
                for (i in 0 until cursor.columnCount) {
                    row.add(readCursorValue(cursor, i))
                }
                rows.add(row)
                count++
            }

            val elapsed = System.currentTimeMillis() - startTime
            return QueryResult(
                columns = columns,
                rows = rows,
                executionTimeMs = elapsed,
                affectedRows = rows.size,
                isSelect = true
            )
        }
    }

    private fun executeDml(db: SQLiteDatabase, sql: String, startTime: Long): QueryResult {
        val upper = sql.trimStart().uppercase()
        var affectedRows = 0

        if (upper.startsWith("INSERT")) {
            try {
                val stmt = db.compileStatement(sql)
                val rowId = stmt.executeInsert()
                affectedRows = if (rowId != -1L) 1 else 0
            } catch (_: Exception) {
                db.execSQL(sql)
                affectedRows = getChanges(db)
            }
        } else if (upper.startsWith("UPDATE") || upper.startsWith("DELETE")) {
            try {
                val stmt = db.compileStatement(sql)
                affectedRows = stmt.executeUpdateDelete()
            } catch (_: Exception) {
                db.execSQL(sql)
                affectedRows = getChanges(db)
            }
        } else {
            // DDL or other commands (CREATE, DROP, ALTER, PRAGMA write, etc.)
            db.execSQL(sql)
            affectedRows = getChanges(db)
        }

        val elapsed = System.currentTimeMillis() - startTime
        return QueryResult(
            columns = emptyList(),
            rows = emptyList(),
            executionTimeMs = elapsed,
            affectedRows = affectedRows,
            isSelect = false
        )
    }

    private fun executeMultipleStatements(
        db: SQLiteDatabase,
        statements: List<String>,
        startTime: Long
    ): QueryResult {
        var totalAffected = 0
        db.beginTransaction()
        try {
            for (stmt in statements) {
                val trimmed = stmt.trim()
                if (trimmed.isEmpty()) continue
                db.execSQL(trimmed)
                totalAffected += getChanges(db)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        val elapsed = System.currentTimeMillis() - startTime
        return QueryResult(
            columns = emptyList(),
            rows = emptyList(),
            executionTimeMs = elapsed,
            affectedRows = totalAffected,
            isSelect = false,
            statementCount = statements.size
        )
    }

    private fun getChanges(db: SQLiteDatabase): Int {
        return try {
            db.rawQuery("SELECT changes()", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        } catch (_: Exception) {
            0
        }
    }

    private fun readCursorValue(cursor: Cursor, columnIndex: Int): String? {
        return if (cursor.isNull(columnIndex)) {
            null
        } else {
            when (cursor.getType(columnIndex)) {
                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(columnIndex).toString()
                Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(columnIndex).toString()
                Cursor.FIELD_TYPE_STRING -> cursor.getString(columnIndex)
                Cursor.FIELD_TYPE_BLOB -> {
                    val blob = cursor.getBlob(columnIndex)
                    "[BLOB ${blob.size} bytes]"
                }
                else -> cursor.getString(columnIndex)
            }
        }
    }

    suspend fun getMetadata(): DatabaseMetadata = withContext(Dispatchers.IO) {
        mutex.withLock {
            val db = getOrOpenDb()
            MetadataExtractor.extract(db, file)
        }
    }

    suspend fun getTableColumns(tableName: String): List<ColumnInfo> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val db = getOrOpenDb()
            MetadataExtractor.extractColumns(db, tableName)
        }
    }

    suspend fun executeInTransaction(action: (SQLiteDatabase) -> Unit): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val db = getOrOpenDb()
            db.beginTransaction()
            try {
                action(db)
                db.setTransactionSuccessful()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun isReadStatement(sql: String): Boolean {
        val firstWord = sql.trimStart().split(Regex("\\s+"), limit = 2).firstOrNull()?.uppercase() ?: ""
        return firstWord in listOf("SELECT", "PRAGMA", "EXPLAIN", "WITH")
    }

    private fun splitStatements(script: String): List<String> {
        val statements = mutableListOf<String>()
        val current = StringBuilder()
        var inSingleQuote = false
        var inDoubleQuote = false

        for (char in script) {
            when (char) {
                '\'' -> {
                    if (!inDoubleQuote) inSingleQuote = !inSingleQuote
                    current.append(char)
                }
                '"' -> {
                    if (!inSingleQuote) inDoubleQuote = !inDoubleQuote
                    current.append(char)
                }
                ';' -> {
                    if (!inSingleQuote && !inDoubleQuote) {
                        val stmt = current.toString().trim()
                        if (stmt.isNotEmpty()) {
                            statements.add(stmt)
                        }
                        current.clear()
                    } else {
                        current.append(char)
                    }
                }
                else -> current.append(char)
            }
        }
        val last = current.toString().trim()
        if (last.isNotEmpty()) {
            statements.add(last)
        }
        return statements
    }
}
