package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DatabaseItem
import com.example.data.model.DatabaseMetadata
import com.example.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DatabaseListUiState(
    val databases: List<DatabaseItem> = emptyList(),
    val isLoading: Boolean = false,
    val showCreateDialog: Boolean = false,
    val userMessage: String? = null,
    val expandedDatabaseIds: Set<Long> = emptySet(),
    val metadataByDatabaseId: Map<Long, DatabaseMetadata> = emptyMap(),
    val loadingMetadataIds: Set<Long> = emptySet()
) {
    val totalSizeBytes: Long
        get() = databases.sumOf { it.sizeBytes }

    val totalTables: Int
        get() = databases.sumOf { it.tableCount }
}

class DatabaseListViewModel(
    private val repository: DatabaseRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _showCreateDialog = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _expandedDatabaseIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _metadataByDatabaseId = MutableStateFlow<Map<Long, DatabaseMetadata>>(emptyMap())
    private val _loadingMetadataIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<DatabaseListUiState> = combine(
        repository.allDatabases,
        _isLoading,
        _showCreateDialog,
        _userMessage,
        _expandedDatabaseIds,
        _metadataByDatabaseId,
        _loadingMetadataIds
    ) { databases, loading, showDialog, message, expanded, metadata, loadingMetadata ->
        DatabaseListUiState(
            databases = databases,
            isLoading = loading,
            showCreateDialog = showDialog,
            userMessage = message,
            expandedDatabaseIds = expanded,
            metadataByDatabaseId = metadata,
            loadingMetadataIds = loadingMetadata
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DatabaseListUiState(isLoading = true)
    )

    fun toggleDatabaseExpanded(databaseId: Long) {
        val current = _expandedDatabaseIds.value.toMutableSet()
        if (!current.add(databaseId)) current.remove(databaseId)
        _expandedDatabaseIds.value = current
        if (databaseId in current && databaseId !in _metadataByDatabaseId.value && databaseId !in _loadingMetadataIds.value) {
            loadDatabaseMetadata(databaseId)
        }
    }

    private fun loadDatabaseMetadata(databaseId: Long) {
        _loadingMetadataIds.value = _loadingMetadataIds.value + databaseId
        viewModelScope.launch {
            try {
                val metadata = repository.getDatabaseMetadata(databaseId)
                _metadataByDatabaseId.value = _metadataByDatabaseId.value + (databaseId to metadata)
            } catch (_: Exception) {
                // The explorer keeps the database node usable even when schema metadata cannot be read.
            } finally {
                _loadingMetadataIds.value = _loadingMetadataIds.value - databaseId
            }
        }
    }

    fun openCreateDialog() {
        _showCreateDialog.value = true
    }

    fun closeCreateDialog() {
        _showCreateDialog.value = false
    }

    fun createDatabase(name: String, description: String, initialSql: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.createLocalDatabase(name, description, initialSql)
                _showCreateDialog.value = false
                _userMessage.value = "Database '$name' created successfully"
            } catch (e: Exception) {
                _userMessage.value = "Error creating database: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openSafDatabase(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.openSafDatabase(uri)
                _userMessage.value = "SQLite database opened from device"
            } catch (e: Exception) {
                _userMessage.value = "Failed to open database: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createSafDatabase(uri: Uri, initialSql: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.createSafDatabase(uri, initialSql)
                _userMessage.value = "New database file saved via Storage Access Framework"
            } catch (e: Exception) {
                _userMessage.value = "Failed to create database: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteDatabase(id: Long, name: String) {
        viewModelScope.launch {
            try {
                repository.deleteDatabase(id)
                _userMessage.value = "Database '$name' deleted"
            } catch (e: Exception) {
                _userMessage.value = "Error deleting database: ${e.message}"
            }
        }
    }

    fun seedSampleDatabases() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.seedSampleDatabases()
                _userMessage.value = "Starter sample databases loaded"
            } catch (e: Exception) {
                _userMessage.value = "Failed to load samples: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissMessage() {
        _userMessage.value = null
    }
}
