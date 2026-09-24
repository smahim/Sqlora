package com.example.data.sqlite

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class SqliteConnectionPool {

    private val connections = ConcurrentHashMap<String, SqliteConnection>()
    private val poolMutex = Mutex()

    suspend fun getOrCreateConnection(file: File): SqliteConnection {
        val path = file.canonicalPath
        return connections.computeIfAbsent(path) {
            SqliteConnection(file)
        }
    }

    suspend fun closeConnection(file: File) {
        val path = file.canonicalPath
        val conn = connections.remove(path)
        conn?.close()
    }

    suspend fun closeAll() {
        poolMutex.withLock {
            for ((_, conn) in connections) {
                conn.close()
            }
            connections.clear()
        }
    }
}
