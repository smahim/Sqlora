package com.example.data.sqlite

import android.database.sqlite.SQLiteDatabase
import com.example.data.model.ColumnInfo
import com.example.data.model.DatabaseMetadata
import com.example.data.model.DatabaseSummary
import com.example.data.model.ForeignKeyInfo
import com.example.data.model.IndexInfo
import com.example.data.model.TableMetadata
import com.example.data.model.TriggerMetadata
import com.example.data.model.ViewMetadata
import java.io.File

object MetadataExtractor {

    fun extract(db: SQLiteDatabase, dbFile: File? = null): DatabaseMetadata {
        val tables = mutableListOf<TableMetadata>()
        val views = mutableListOf<ViewMetadata>()
        val triggers = mutableListOf<TriggerMetadata>()

        // 1. Discover tables and views from sqlite_master
        val masterQuery = """
            SELECT name, type, sql 
            FROM sqlite_master 
            WHERE name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_metadata'
            ORDER BY type, name ASC
        """.trimIndent()

        db.rawQuery(masterQuery, null).use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            val typeIdx = cursor.getColumnIndex("type")
            val sqlIdx = cursor.getColumnIndex("sql")

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx) ?: continue
                val type = cursor.getString(typeIdx)?.lowercase() ?: ""
                val sql = cursor.getString(sqlIdx) ?: ""

                when (type) {
                    "table" -> {
                        val tableMeta = extractTableDetails(db, name, sql)
                        tables.add(tableMeta)
                    }
                    "view" -> {
                        val cols = extractColumns(db, name)
                        views.add(ViewMetadata(name = name, sql = sql, columns = cols))
                    }
                }
            }
        }

        // 2. Discover triggers
        val triggerQuery = "SELECT name, tbl_name, sql FROM sqlite_master WHERE type = 'trigger' ORDER BY name ASC"
        try {
            db.rawQuery(triggerQuery, null).use { cursor ->
                val nameIdx = cursor.getColumnIndex("name")
                val tblIdx = cursor.getColumnIndex("tbl_name")
                val sqlIdx = cursor.getColumnIndex("sql")

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx) ?: ""
                    val tbl = cursor.getString(tblIdx) ?: ""
                    val sql = cursor.getString(sqlIdx) ?: ""
                    triggers.add(
                        TriggerMetadata(
                            name = name,
                            targetTable = tbl,
                            event = parseTriggerEvent(sql),
                            timing = parseTriggerTiming(sql),
                            sql = sql
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 3. Database Summary Info
        val summary = extractSummary(db, dbFile, tables.size, views.size, triggers.size)

        return DatabaseMetadata(
            summary = summary,
            tables = tables,
            views = views,
            triggers = triggers
        )
    }

    private fun extractTableDetails(db: SQLiteDatabase, tableName: String, sql: String): TableMetadata {
        val columns = extractColumns(db, tableName)
        val primaryKeys = columns.filter { it.isPrimaryKey }.map { it.name }
        val foreignKeys = extractForeignKeys(db, tableName)
        val indexes = extractIndexes(db, tableName)

        var rowCount: Long = 0
        try {
            db.rawQuery("SELECT COUNT(*) FROM \"$tableName\"", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    rowCount = cursor.getLong(0)
                }
            }
        } catch (_: Exception) {}

        return TableMetadata(
            name = tableName,
            rowCount = rowCount,
            sql = sql,
            columns = columns,
            primaryKeys = primaryKeys,
            foreignKeys = foreignKeys,
            indexes = indexes
        )
    }

    fun extractColumns(db: SQLiteDatabase, tableName: String): List<ColumnInfo> {
        val columns = mutableListOf<ColumnInfo>()
        try {
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
        } catch (_: Exception) {}
        return columns
    }

    private fun extractForeignKeys(db: SQLiteDatabase, tableName: String): List<ForeignKeyInfo> {
        val fks = mutableListOf<ForeignKeyInfo>()
        try {
            db.rawQuery("PRAGMA foreign_key_list(\"$tableName\")", null).use { cursor ->
                val idIdx = cursor.getColumnIndex("id")
                val seqIdx = cursor.getColumnIndex("seq")
                val tableIdx = cursor.getColumnIndex("table")
                val fromIdx = cursor.getColumnIndex("from")
                val toIdx = cursor.getColumnIndex("to")
                val onUpdateIdx = cursor.getColumnIndex("on_update")
                val onDeleteIdx = cursor.getColumnIndex("on_delete")

                while (cursor.moveToNext()) {
                    fks.add(
                        ForeignKeyInfo(
                            id = if (idIdx >= 0) cursor.getInt(idIdx) else 0,
                            seq = if (seqIdx >= 0) cursor.getInt(seqIdx) else 0,
                            targetTable = if (tableIdx >= 0) cursor.getString(tableIdx) ?: "" else "",
                            fromColumn = if (fromIdx >= 0) cursor.getString(fromIdx) ?: "" else "",
                            toColumn = if (toIdx >= 0) cursor.getString(toIdx) ?: "" else "",
                            onUpdate = if (onUpdateIdx >= 0) cursor.getString(onUpdateIdx) ?: "NO ACTION" else "NO ACTION",
                            onDelete = if (onDeleteIdx >= 0) cursor.getString(onDeleteIdx) ?: "NO ACTION" else "NO ACTION"
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return fks
    }

    private fun extractIndexes(db: SQLiteDatabase, tableName: String): List<IndexInfo> {
        val indexes = mutableListOf<IndexInfo>()
        try {
            db.rawQuery("PRAGMA index_list(\"$tableName\")", null).use { cursor ->
                val nameIdx = cursor.getColumnIndex("name")
                val uniqueIdx = cursor.getColumnIndex("unique")
                val originIdx = cursor.getColumnIndex("origin")

                while (cursor.moveToNext()) {
                    val idxName = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                    val isUnique = if (uniqueIdx >= 0) cursor.getInt(uniqueIdx) == 1 else false
                    val origin = if (originIdx >= 0) cursor.getString(originIdx) ?: "c" else "c"

                    val idxColumns = mutableListOf<String>()
                    try {
                        db.rawQuery("PRAGMA index_info(\"$idxName\")", null).use { infoCursor ->
                            val colNameIdx = infoCursor.getColumnIndex("name")
                            while (infoCursor.moveToNext()) {
                                if (colNameIdx >= 0) {
                                    infoCursor.getString(colNameIdx)?.let { idxColumns.add(it) }
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    indexes.add(
                        IndexInfo(
                            name = idxName,
                            isUnique = isUnique,
                            origin = origin,
                            columns = idxColumns
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return indexes
    }

    private fun extractSummary(
        db: SQLiteDatabase,
        dbFile: File?,
        tableCount: Int,
        viewCount: Int,
        triggerCount: Int
    ): DatabaseSummary {
        var sqliteVersion = "3"
        try {
            db.rawQuery("SELECT sqlite_version()", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    sqliteVersion = cursor.getString(0) ?: "3"
                }
            }
        } catch (_: Exception) {}

        var pageSize = 4096L
        try {
            db.rawQuery("PRAGMA page_size", null).use { cursor ->
                if (cursor.moveToFirst()) pageSize = cursor.getLong(0)
            }
        } catch (_: Exception) {}

        var pageCount = 0L
        try {
            db.rawQuery("PRAGMA page_count", null).use { cursor ->
                if (cursor.moveToFirst()) pageCount = cursor.getLong(0)
            }
        } catch (_: Exception) {}

        var encoding = "UTF-8"
        try {
            db.rawQuery("PRAGMA encoding", null).use { cursor ->
                if (cursor.moveToFirst()) encoding = cursor.getString(0) ?: "UTF-8"
            }
        } catch (_: Exception) {}

        var journalMode = "wal"
        try {
            db.rawQuery("PRAGMA journal_mode", null).use { cursor ->
                if (cursor.moveToFirst()) journalMode = cursor.getString(0) ?: "unknown"
            }
        } catch (_: Exception) {}

        var autoVacuum = "0"
        try {
            db.rawQuery("PRAGMA auto_vacuum", null).use { cursor ->
                if (cursor.moveToFirst()) autoVacuum = cursor.getString(0) ?: "0"
            }
        } catch (_: Exception) {}

        var userVersion = 0
        try {
            db.rawQuery("PRAGMA user_version", null).use { cursor ->
                if (cursor.moveToFirst()) userVersion = cursor.getInt(0)
            }
        } catch (_: Exception) {}

        var indexCount = 0
        try {
            db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = 'index'", null).use { cursor ->
                if (cursor.moveToFirst()) indexCount = cursor.getInt(0)
            }
        } catch (_: Exception) {}

        val fileSize = dbFile?.length() ?: (pageCount * pageSize)

        return DatabaseSummary(
            sqliteVersion = sqliteVersion,
            pageSizeBytes = pageSize,
            pageCount = pageCount,
            fileSizeBytes = fileSize,
            encoding = encoding,
            journalMode = journalMode,
            autoVacuum = autoVacuum,
            userVersion = userVersion,
            tableCount = tableCount,
            viewCount = viewCount,
            indexCount = indexCount,
            triggerCount = triggerCount
        )
    }

    private fun parseTriggerEvent(sql: String): String {
        val upper = sql.uppercase()
        return when {
            upper.contains("INSERT") -> "INSERT"
            upper.contains("UPDATE") -> "UPDATE"
            upper.contains("DELETE") -> "DELETE"
            else -> "ACTION"
        }
    }

    private fun parseTriggerTiming(sql: String): String {
        val upper = sql.uppercase()
        return when {
            upper.contains("BEFORE") -> "BEFORE"
            upper.contains("AFTER") -> "AFTER"
            upper.contains("INSTEAD OF") -> "INSTEAD OF"
            else -> "AFTER"
        }
    }
}
