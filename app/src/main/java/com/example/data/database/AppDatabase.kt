package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.DatabaseDao
import com.example.data.database.dao.QueryHistoryDao
import com.example.data.database.dao.SavedQueryDao
import com.example.data.database.entity.DatabaseEntity
import com.example.data.database.entity.QueryHistoryEntity
import com.example.data.database.entity.SavedQueryEntity

@Database(
    entities = [
        DatabaseEntity::class,
        QueryHistoryEntity::class,
        SavedQueryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun databaseDao(): DatabaseDao
    abstract fun queryHistoryDao(): QueryHistoryDao
    abstract fun savedQueryDao(): SavedQueryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sqlitestudio_metadata.db"
                )
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
