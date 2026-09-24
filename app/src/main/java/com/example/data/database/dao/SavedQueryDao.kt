package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.SavedQueryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedQueryDao {
    @Query("SELECT * FROM saved_queries ORDER BY createdAt DESC")
    fun getAllSavedQueries(): Flow<List<SavedQueryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SavedQueryEntity): Long

    @Query("UPDATE saved_queries SET title = :title, sqlQuery = :sqlText WHERE id = :id")
    suspend fun updateSavedQuery(id: Long, title: String, sqlText: String)

    @Query("DELETE FROM saved_queries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
