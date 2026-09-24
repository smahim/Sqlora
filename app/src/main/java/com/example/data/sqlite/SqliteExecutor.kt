package com.example.data.sqlite

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.example.data.model.ColumnInfo
import com.example.data.model.QueryResult
import com.example.data.model.TableInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SqliteExecutor {

    suspend fun getTables(dbFile: File): List<TableInfo> = withContext(Dispatchers.IO) {
        if (!dbFile.exists()) return@withContext emptyList()
        val tables = mutableListOf<TableInfo>()
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val query = """
                SELECT name, type, sql 
                FROM sqlite_master 
                WHERE type IN ('table', 'view') 
                  AND name NOT LIKE 'sqlite_%' 
                  AND name NOT LIKE 'android_metadata'
                ORDER BY name ASC
            """.trimIndent()

            db.rawQuery(query, null).use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                val typeIndex = cursor.getColumnIndex("type")
                val sqlIndex = cursor.getColumnIndex("sql")

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex) ?: ""
                    val type = cursor.getString(typeIndex)?.uppercase() ?: "TABLE"
                    val sql = cursor.getString(sqlIndex) ?: ""

                    var rowCount: Long = 0
                    var colCount: Int = 0

                    // Calculate column count
                    try {
                        db.rawQuery("PRAGMA table_info(\"$name\")", null).use { pragmaCursor ->
                            colCount = pragmaCursor.count
                        }
                    } catch (_: Exception) {}

                    // Calculate row count
                    try {
                        db.rawQuery("SELECT COUNT(*) FROM \"$name\"", null).use { countCursor ->
                            if (countCursor.moveToFirst()) {
                                rowCount = countCursor.getLong(0)
                            }
                        }
                    } catch (_: Exception) {}

                    tables.add(
                        TableInfo(
                            name = name,
                            type = type,
                            rowCount = rowCount,
                            columnCount = colCount,
                            sql = sql
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                db?.close()
            } catch (_: Exception) {}
        }
        tables
    }

    suspend fun getTableColumns(dbFile: File, tableName: String): List<ColumnInfo> = withContext(Dispatchers.IO) {
        if (!dbFile.exists()) return@withContext emptyList()
        val columns = mutableListOf<ColumnInfo>()
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            db.rawQuery("PRAGMA table_info(\"$tableName\")", null).use { cursor ->
                val cidIdx = cursor.getColumnIndex("cid")
                val nameIdx = cursor.getColumnIndex("name")
                val typeIdx = cursor.getColumnIndex("type")
                val notNullIdx = cursor.getColumnIndex("notnull")
                val dfltIdx = cursor.getColumnIndex("dflt_value")
                val pkIdx = cursor.getColumnIndex("pk")

                while (cursor.moveToNext()) {
                    val cid = if (cidIdx >= 0) cursor.getInt(cidIdx) else 0
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                    val type = if (typeIdx >= 0) cursor.getString(typeIdx) ?: "TEXT" else "TEXT"
                    val notNull = if (notNullIdx >= 0) cursor.getInt(notNullIdx) == 1 else false
                    val dfltValue = if (dfltIdx >= 0) cursor.getString(dfltIdx) else null
                    val isPk = if (pkIdx >= 0) cursor.getInt(pkIdx) > 0 else false

                    columns.add(
                        ColumnInfo(
                            cid = cid,
                            name = name,
                            type = type.ifBlank { "ANY" },
                            isNotNull = notNull,
                            defaultValue = dfltValue,
                            isPrimaryKey = isPk
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                db?.close()
            } catch (_: Exception) {}
        }
        columns
    }

    suspend fun getTableRows(
        dbFile: File,
        tableName: String,
        limit: Int = 50,
        offset: Int = 0
    ): QueryResult = withContext(Dispatchers.IO) {
        val query = "SELECT * FROM \"$tableName\" LIMIT $limit OFFSET $offset"
        executeQuery(dbFile, query)
    }

    suspend fun executeQuery(dbFile: File, rawSql: String): QueryResult = withContext(Dispatchers.IO) {
        if (!dbFile.exists()) {
            return@withContext QueryResult(errorMessage = "Database file does not exist.")
        }

        val trimmed = rawSql.trim()
        if (trimmed.isBlank()) {
            return@withContext QueryResult(errorMessage = "Empty SQL statement.")
        }

        val isReadQuery = isReadStatement(trimmed)
        val startTime = System.currentTimeMillis()
        var db: SQLiteDatabase? = null

        try {
            val flags = if (isReadQuery) SQLiteDatabase.OPEN_READONLY else SQLiteDatabase.OPEN_READWRITE
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, flags)

            if (isReadQuery) {
                db.rawQuery(trimmed, null).use { cursor ->
                    val columns = cursor.columnNames.toList()
                    val rows = mutableListOf<List<String?>>()

                    val maxRows = 200 // Prevent memory overload on mobile
                    var count = 0
                    while (cursor.moveToNext() && count < maxRows) {
                        val row = mutableListOf<String?>()
                        for (i in 0 until cursor.columnCount) {
                            row.add(getCursorValue(cursor, i))
                        }
                        rows.add(row)
                        count++
                    }

                    val elapsed = System.currentTimeMillis() - startTime
                    QueryResult(
                        columns = columns,
                        rows = rows,
                        executionTimeMs = elapsed,
                        affectedRows = rows.size,
                        isSelect = true,
                        errorMessage = null
                    )
                }
            } else {
                db.beginTransaction()
                try {
                    db.execSQL(trimmed)
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }

                // Check changes
                var changes = 0
                try {
                    db.rawQuery("SELECT changes()", null).use { cursor ->
                        if (cursor.moveToFirst()) {
                            changes = cursor.getInt(0)
                        }
                    }
                } catch (_: Exception) {}

                val elapsed = System.currentTimeMillis() - startTime
                QueryResult(
                    columns = emptyList(),
                    rows = emptyList(),
                    executionTimeMs = elapsed,
                    affectedRows = changes,
                    isSelect = false,
                    errorMessage = null
                )
            }
        } catch (e: SQLiteException) {
            val elapsed = System.currentTimeMillis() - startTime
            QueryResult(
                executionTimeMs = elapsed,
                isSelect = isReadQuery,
                errorMessage = e.message ?: "SQLite syntax or runtime error"
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            QueryResult(
                executionTimeMs = elapsed,
                isSelect = isReadQuery,
                errorMessage = e.localizedMessage ?: "Unexpected error during query execution"
            )
        } finally {
            try {
                db?.close()
            } catch (_: Exception) {}
        }
    }

    private fun isReadStatement(sql: String): Boolean {
        val firstWord = sql.trimStart().split(Regex("\\s+"), limit = 2).firstOrNull()?.uppercase() ?: ""
        return firstWord in listOf("SELECT", "PRAGMA", "EXPLAIN", "WITH")
    }

    private fun getCursorValue(cursor: Cursor, columnIndex: Int): String? {
        return if (cursor.isNull(columnIndex)) {
            null
        } else {
            when (cursor.getType(columnIndex)) {
                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(columnIndex).toString()
                Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(columnIndex).toString()
                Cursor.FIELD_TYPE_STRING -> cursor.getString(columnIndex)
                Cursor.FIELD_TYPE_BLOB -> "[BLOB ${cursor.getBlob(columnIndex).size} bytes]"
                else -> cursor.getString(columnIndex)
            }
        }
    }

    suspend fun createDatabase(dbFile: File, initialSql: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            dbFile.parentFile?.mkdirs()
            val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            if (!initialSql.isNullOrBlank()) {
                val statements = initialSql.split(";").map { it.trim() }.filter { it.isNotEmpty() }
                db.beginTransaction()
                try {
                    for (stmt in statements) {
                        db.execSQL(stmt)
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
            db.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteDatabaseFile(dbFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            SQLiteDatabase.deleteDatabase(dbFile)
        } catch (e: Exception) {
            e.printStackTrace()
            dbFile.delete()
        }
    }
}
