package com.example.repository

import android.content.Context
import android.net.Uri
import com.example.data.database.AppDatabase
import com.example.data.database.entity.DatabaseEntity
import com.example.data.database.entity.QueryHistoryEntity
import com.example.data.database.entity.SavedQueryEntity
import com.example.data.model.ColumnInfo
import com.example.data.model.DatabaseItem
import com.example.data.model.DatabaseMetadata
import com.example.data.model.QueryHistoryItem
import com.example.data.model.QueryResult
import com.example.data.model.SavedQueryItem
import com.example.data.model.SqlErrorDetails
import com.example.data.sqlite.SafDatabaseHelper
import com.example.data.sqlite.SqlErrorParser
import com.example.data.sqlite.SqliteConnectionPool
import com.example.util.SampleDataSeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

interface DatabaseRepository {
    val allDatabases: Flow<List<DatabaseItem>>

    fun getDatabaseById(id: Long): Flow<DatabaseItem?>

    suspend fun createLocalDatabase(name: String, description: String = "", initialSql: String? = null): Long

    suspend fun openSafDatabase(uri: Uri): Long

    suspend fun createSafDatabase(uri: Uri, initialSql: String? = null): Long

    suspend fun syncSafDatabase(databaseId: Long): Boolean

    suspend fun exportDatabaseToSaf(databaseId: Long, destinationUri: Uri): Boolean

    suspend fun deleteDatabase(id: Long)

    suspend fun closeDatabase(id: Long)

    suspend fun getDatabaseMetadata(databaseId: Long): DatabaseMetadata

    suspend fun getTableColumns(databaseId: Long, tableName: String): List<ColumnInfo>

    suspend fun getTableRows(databaseId: Long, tableName: String, limit: Int = 50, offset: Int = 0): QueryResult

    suspend fun executeSql(databaseId: Long, sql: String): QueryResult

    fun getHistoryForDatabase(databaseId: Long): Flow<List<QueryHistoryItem>>

    fun getAllHistory(): Flow<List<QueryHistoryItem>>

    suspend fun deleteHistoryItem(id: Long)

    suspend fun clearHistory(databaseId: Long)

    fun getSavedQueries(): Flow<List<SavedQueryItem>>

    suspend fun saveQuery(title: String, sql: String, databaseId: Long?)

    suspend fun updateSavedQuery(id: Long, title: String, sql: String)

    suspend fun deleteSavedQuery(id: Long)

    fun getAllFolders(): Flow<List<String>>

    suspend fun updateDatabaseFolder(id: Long, folder: String)

    suspend fun renameDatabase(id: Long, newName: String)

    suspend fun seedSampleDatabases()
}

class DatabaseRepositoryImpl(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val connectionPool: SqliteConnectionPool = SqliteConnectionPool()
) : DatabaseRepository {

    private val databaseDao = appDatabase.databaseDao()
    private val queryHistoryDao = appDatabase.queryHistoryDao()
    private val savedQueryDao = appDatabase.savedQueryDao()

    override val allDatabases: Flow<List<DatabaseItem>> = databaseDao.getAllDatabases().map { entities ->
        entities.map { entity ->
            val file = File(entity.filePath)
            val size = if (file.exists()) file.length() else 0L
            DatabaseItem(
                id = entity.id,
                name = entity.name,
                filePath = entity.filePath,
                description = entity.description,
                folder = entity.folder,
                tableCount = 0,
                sizeBytes = size,
                isSample = entity.isSample,
                uriString = entity.uriString,
                isSafBacked = entity.isSafBacked,
                createdAt = entity.createdAt,
                lastModified = entity.lastModified
            )
        }
    }

    override fun getDatabaseById(id: Long): Flow<DatabaseItem?> = databaseDao.getDatabaseById(id).map { entity ->
        entity?.let {
            val file = File(it.filePath)
            val size = if (file.exists()) file.length() else 0L
            DatabaseItem(
                id = it.id,
                name = it.name,
                filePath = it.filePath,
                description = it.description,
                folder = it.folder,
                tableCount = 0,
                sizeBytes = size,
                isSample = it.isSample,
                uriString = it.uriString,
                isSafBacked = it.isSafBacked,
                createdAt = it.createdAt,
                lastModified = it.lastModified
            )
        }
    }

    override suspend fun createLocalDatabase(
        name: String,
        description: String,
        initialSql: String?
    ): Long = withContext(Dispatchers.IO) {
        val safeName = name.trim().replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        val fileName = if (safeName.endsWith(".db")) safeName else "$safeName.db"
        val dbDir = File(context.filesDir, "databases")
        dbDir.mkdirs()
        val dbFile = File(dbDir, fileName)

        val entity = DatabaseEntity(
            name = name.trim(),
            filePath = dbFile.absolutePath,
            description = description.trim(),
            isSample = false,
            isSafBacked = false
        )
        val id = databaseDao.insert(entity)

        val conn = connectionPool.getOrCreateConnection(dbFile)
        conn.open()

        if (!initialSql.isNullOrBlank()) {
            conn.executeSql(initialSql)
        }

        id
    }

    override suspend fun openSafDatabase(uri: Uri): Long = withContext(Dispatchers.IO) {
        SafDatabaseHelper.takePersistablePermissions(context, uri)
        val displayName = SafDatabaseHelper.getDisplayName(context, uri)

        val safWorkDir = File(context.filesDir, "saf_working_dbs")
        safWorkDir.mkdirs()
        val workingFile = File(safWorkDir, "${UUID.randomUUID()}_$displayName")

        val copied = SafDatabaseHelper.copyUriToWorkingFile(context, uri, workingFile)
        if (!copied) {
            throw IllegalArgumentException("Failed to read SQLite file from chosen location.")
        }

        val entity = DatabaseEntity(
            name = displayName.removeSuffix(".db").removeSuffix(".sqlite").removeSuffix(".sqlite3"),
            filePath = workingFile.absolutePath,
            description = "Opened from device via Storage Access Framework",
            isSample = false,
            uriString = uri.toString(),
            isSafBacked = true
        )
        databaseDao.insert(entity)
    }

    override suspend fun createSafDatabase(uri: Uri, initialSql: String?): Long = withContext(Dispatchers.IO) {
        SafDatabaseHelper.takePersistablePermissions(context, uri)
        val displayName = SafDatabaseHelper.getDisplayName(context, uri)

        val safWorkDir = File(context.filesDir, "saf_working_dbs")
        safWorkDir.mkdirs()
        val workingFile = File(safWorkDir, "${UUID.randomUUID()}_$displayName")

        val conn = connectionPool.getOrCreateConnection(workingFile)
        conn.open()
        if (!initialSql.isNullOrBlank()) {
            conn.executeSql(initialSql)
        }

        SafDatabaseHelper.syncWorkingFileToUri(context, workingFile, uri)

        val entity = DatabaseEntity(
            name = displayName.removeSuffix(".db").removeSuffix(".sqlite").removeSuffix(".sqlite3"),
            filePath = workingFile.absolutePath,
            description = "Created in Storage Access Framework location",
            isSample = false,
            uriString = uri.toString(),
            isSafBacked = true
        )
        databaseDao.insert(entity)
    }

    override suspend fun syncSafDatabase(databaseId: Long): Boolean = withContext(Dispatchers.IO) {
        val entity = databaseDao.getDatabaseById(databaseId).firstOrNull() ?: return@withContext false
        val uriStr = entity.uriString ?: return@withContext false
        val file = File(entity.filePath)
        if (!file.exists()) return@withContext false
        val uri = Uri.parse(uriStr)
        SafDatabaseHelper.syncWorkingFileToUri(context, file, uri)
    }

    override suspend fun exportDatabaseToSaf(databaseId: Long, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val entity = databaseDao.getDatabaseById(databaseId).firstOrNull() ?: return@withContext false
        val file = File(entity.filePath)
        if (!file.exists()) return@withContext false
        SafDatabaseHelper.syncWorkingFileToUri(context, file, destinationUri)
    }

    override suspend fun deleteDatabase(id: Long) = withContext(Dispatchers.IO) {
        val entity = databaseDao.getDatabaseById(id).firstOrNull()
        if (entity != null) {
            val file = File(entity.filePath)
            connectionPool.closeConnection(file)
            if (file.exists()) {
                file.delete()
                File("${file.absolutePath}-wal").delete()
                File("${file.absolutePath}-shm").delete()
                File("${file.absolutePath}-journal").delete()
            }
            databaseDao.deleteById(id)
            queryHistoryDao.clearHistoryForDatabase(id)
        }
    }

    override suspend fun closeDatabase(id: Long) = withContext(Dispatchers.IO) {
        val entity = databaseDao.getDatabaseById(id).firstOrNull()
        if (entity != null) {
            val file = File(entity.filePath)
            connectionPool.closeConnection(file)
        }
    }

    override suspend fun getDatabaseMetadata(databaseId: Long): DatabaseMetadata = withContext(Dispatchers.IO) {
        val entity = databaseDao.getDatabaseById(databaseId).firstOrNull()
            ?: throw IllegalArgumentException("Database with ID $databaseId not found")
        val file = File(entity.filePath)
        val conn = connectionPool.getOrCreateConnection(file)
        conn.getMetadata()
    }

    override suspend fun getTableColumns(databaseId: Long, tableName: String): List<ColumnInfo> =
        withContext(Dispatchers.IO) {
            val entity = databaseDao.getDatabaseById(databaseId).firstOrNull() ?: return@withContext emptyList()
            val file = File(entity.filePath)
            val conn = connectionPool.getOrCreateConnection(file)
            conn.getTableColumns(tableName)
        }

    override suspend fun getTableRows(
        databaseId: Long,
        tableName: String,
        limit: Int,
        offset: Int
    ): QueryResult = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM \"$tableName\" LIMIT $limit OFFSET $offset;"
        executeSql(databaseId, sql)
    }

    override suspend fun executeSql(databaseId: Long, sql: String): QueryResult = withContext(Dispatchers.IO) {
        val entity = databaseDao.getDatabaseById(databaseId).firstOrNull()
            ?: return@withContext QueryResult(
                errorMessage = "Database not found",
                errorDetails = SqlErrorDetails(
                    title = "Database Not Found",
                    userExplanation = "The requested database registration could not be located.",
                    technicalMessage = "Database ID $databaseId not found in registry"
                )
            )

        val file = File(entity.filePath)
        val conn = connectionPool.getOrCreateConnection(file)
        val result = conn.executeSql(sql)

        // Record history in Room
        try {
            queryHistoryDao.insert(
                QueryHistoryEntity(
                    databaseId = databaseId,
                    sqlQuery = sql,
                    isSuccess = result.isSuccess,
                    executionTimeMs = result.executionTimeMs,
                    rowsAffected = result.affectedRows,
                    errorMessage = result.errorMessage
                )
            )
        } catch (_: Exception) {}

        // If SAF backed and statement was write, automatically sync back if possible
        if (entity.isSafBacked && !result.isSelect && result.isSuccess && entity.uriString != null) {
            try {
                SafDatabaseHelper.syncWorkingFileToUri(context, file, Uri.parse(entity.uriString))
            } catch (_: Exception) {}
        }

        result
    }

    override fun getHistoryForDatabase(databaseId: Long): Flow<List<QueryHistoryItem>> =
        queryHistoryDao.getHistoryForDatabase(databaseId).map { list ->
            list.map {
                QueryHistoryItem(
                    id = it.id,
                    databaseId = it.databaseId,
                    sqlQuery = it.sqlQuery,
                    executedAt = it.executedAt,
                    executionTimeMs = it.executionTimeMs,
                    isSuccess = it.isSuccess,
                    rowsAffected = it.rowsAffected,
                    errorMessage = it.errorMessage
                )
            }
        }

    override suspend fun clearHistory(databaseId: Long) = withContext(Dispatchers.IO) {
        queryHistoryDao.clearHistoryForDatabase(databaseId)
    }

    override fun getSavedQueries(): Flow<List<SavedQueryItem>> = savedQueryDao.getAllSavedQueries().map { list ->
        list.map {
            SavedQueryItem(
                id = it.id,
                title = it.title,
                sqlQuery = it.sqlQuery,
                databaseId = it.databaseId,
                createdAt = it.createdAt
            )
        }
    }

    override suspend fun saveQuery(title: String, sql: String, databaseId: Long?) {
        withContext(Dispatchers.IO) {
            savedQueryDao.insert(
                SavedQueryEntity(
                    title = title,
                    sqlQuery = sql,
                    databaseId = databaseId
                )
            )
        }
    }

    override fun getAllHistory(): Flow<List<QueryHistoryItem>> = queryHistoryDao.getAllHistory().map { entities ->
        entities.map {
            QueryHistoryItem(
                id = it.id,
                databaseId = it.databaseId,
                sqlQuery = it.sqlQuery,
                executedAt = it.executedAt,
                executionTimeMs = it.executionTimeMs,
                isSuccess = it.isSuccess,
                rowsAffected = it.rowsAffected,
                errorMessage = it.errorMessage
            )
        }
    }

    override suspend fun deleteHistoryItem(id: Long) = withContext(Dispatchers.IO) {
        queryHistoryDao.deleteById(id)
    }

    override suspend fun updateSavedQuery(id: Long, title: String, sql: String) = withContext(Dispatchers.IO) {
        savedQueryDao.updateSavedQuery(id, title, sql)
    }

    override fun getAllFolders(): Flow<List<String>> = databaseDao.getAllFolders()

    override suspend fun updateDatabaseFolder(id: Long, folder: String) = withContext(Dispatchers.IO) {
        databaseDao.updateFolder(id, folder.trim().ifBlank { "General" })
    }

    override suspend fun renameDatabase(id: Long, newName: String) = withContext(Dispatchers.IO) {
        databaseDao.updateName(id, newName.trim())
    }

    override suspend fun deleteSavedQuery(id: Long) = withContext(Dispatchers.IO) {
        savedQueryDao.deleteById(id)
    }

    override suspend fun seedSampleDatabases() = withContext(Dispatchers.IO) {
        val existing = databaseDao.getAllDatabases().firstOrNull() ?: emptyList()
        if (existing.any { it.isSample }) return@withContext

        val dbDir = File(context.filesDir, "databases")
        dbDir.mkdirs()

        // 1. E-Commerce Inventory Sample
        val ecommerceFile = File(dbDir, "ecommerce_inventory.db")
        val ecomConn = connectionPool.getOrCreateConnection(ecommerceFile)
        ecomConn.open()
        ecomConn.executeSql(SampleDataSeeder.ECOMMERCE_SQL)

        databaseDao.insert(
            DatabaseEntity(
                name = "E-Commerce Inventory",
                filePath = ecommerceFile.absolutePath,
                description = "Starter sample: Products, categories, customers, and orders",
                isSample = true,
                isSafBacked = false
            )
        )

        // 2. Project Tracker Sample
        val projectsFile = File(dbDir, "project_hub.db")
        val projConn = connectionPool.getOrCreateConnection(projectsFile)
        projConn.open()
        projConn.executeSql(SampleDataSeeder.PROJECT_HUB_SQL)

        databaseDao.insert(
            DatabaseEntity(
                name = "Project Tracker",
                filePath = projectsFile.absolutePath,
                description = "Starter sample: Projects, sprints, and task progress tracking",
                isSample = true,
                isSafBacked = false
            )
        )
    }
}
