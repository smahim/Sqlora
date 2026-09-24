package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.QueryHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueryHistoryDao {
    @Query("SELECT * FROM query_history WHERE databaseId = :databaseId ORDER BY executedAt DESC LIMIT 100")
    fun getHistoryForDatabase(databaseId: Long): Flow<List<QueryHistoryEntity>>

    @Query("SELECT * FROM query_history ORDER BY executedAt DESC LIMIT 100")
    fun getAllHistory(): Flow<List<QueryHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QueryHistoryEntity): Long

    @Query("DELETE FROM query_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM query_history WHERE databaseId = :databaseId")
    suspend fun clearHistoryForDatabase(databaseId: Long)

    @Query("DELETE FROM query_history")
    suspend fun clearAllHistory()
}
