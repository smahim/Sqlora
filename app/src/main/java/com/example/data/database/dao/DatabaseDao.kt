package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.DatabaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DatabaseDao {
    @Query("SELECT * FROM databases ORDER BY lastModified DESC")
    fun getAllDatabases(): Flow<List<DatabaseEntity>>

    @Query("SELECT * FROM databases WHERE id = :id LIMIT 1")
    fun getDatabaseById(id: Long): Flow<DatabaseEntity?>

    @Query("SELECT * FROM databases WHERE id = :id LIMIT 1")
    suspend fun getDatabaseByIdSync(id: Long): DatabaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DatabaseEntity): Long

    @Update
    suspend fun update(entity: DatabaseEntity)

    @Query("DELETE FROM databases WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE databases SET folder = :folder WHERE id = :id")
    suspend fun updateFolder(id: Long, folder: String)

    @Query("UPDATE databases SET name = :newName WHERE id = :id")
    suspend fun updateName(id: Long, newName: String)

    @Query("SELECT DISTINCT folder FROM databases WHERE folder IS NOT NULL AND folder != ''")
    fun getAllFolders(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM databases")
    suspend fun getDatabaseCount(): Int
}
