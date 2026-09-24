package com.example.data.sqlite

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object SafDatabaseHelper {

    private const val SQLITE_HEADER = "SQLite format 3"

    suspend fun getDisplayName(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIdx)
                }
            }
        } catch (_: Exception) {}

        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_database.db"
        }
        if (!name!!.endsWith(".db", ignoreCase = true) &&
            !name!!.endsWith(".sqlite", ignoreCase = true) &&
            !name!!.endsWith(".sqlite3", ignoreCase = true)
        ) {
            name += ".db"
        }
        name!!
    }

    suspend fun takePersistablePermissions(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Some document providers might not support persistable permissions
        }
    }

    suspend fun copyUriToWorkingFile(context: Context, uri: Uri, destinationFile: File): Boolean =
        withContext(Dispatchers.IO) {
            try {
                destinationFile.parentFile?.mkdirs()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun syncWorkingFileToUri(context: Context, sourceFile: File, destinationUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            if (!sourceFile.exists()) return@withContext false
            try {
                context.contentResolver.openOutputStream(destinationUri, "rwt")?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun isSqliteDatabase(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() < 16) return@withContext false
        try {
            FileInputStream(file).use { stream ->
                val header = ByteArray(16)
                val read = stream.read(header)
                if (read >= 16) {
                    val headerString = String(header, Charsets.US_ASCII)
                    return@withContext headerString.startsWith(SQLITE_HEADER)
                }
            }
        } catch (_: Exception) {}
        false
    }
}
