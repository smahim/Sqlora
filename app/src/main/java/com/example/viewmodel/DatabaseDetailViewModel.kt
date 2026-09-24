package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DatabaseItem
import com.example.data.model.DatabaseMetadata
import com.example.data.model.TableInfo
import com.example.data.model.TableMetadata
import com.example.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DatabaseDetailUiState(
    val database: DatabaseItem? = null,
    val metadata: DatabaseMetadata? = null,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val activeTab: Int = 0, // 0: Tables, 1: Views, 2: Triggers, 3: DB Info
    val error: String? = null,
    val userMessage: String? = null
) {
    val filteredTables: List<TableMetadata>
        get() {
            val list = metadata?.tables ?: emptyList()
            if (searchQuery.isBlank()) return list
            return list.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

    val filteredViews: List<com.example.data.model.ViewMetadata>
        get() {
            val list = metadata?.views ?: emptyList()
            if (searchQuery.isBlank()) return list
            return list.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
}

class DatabaseDetailViewModel(
    private val databaseId: Long,
    private val repository: DatabaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DatabaseDetailUiState(isLoading = true))
    val uiState: StateFlow<DatabaseDetailUiState> = _uiState.asStateFlow()

    init {
        loadDatabase()
    }

    fun loadDatabase() {
        viewModelScope.launch {
            repository.getDatabaseById(databaseId).collect { db ->
                _uiState.update { it.copy(database = db) }
                if (db != null) {
                    loadMetadata()
                }
            }
        }
    }

    fun loadMetadata() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val meta = repository.getDatabaseMetadata(databaseId)
                _uiState.update {
                    it.copy(
                        metadata = meta,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun exportToSaf(uri: Uri) {
        viewModelScope.launch {
            try {
                val success = repository.exportDatabaseToSaf(databaseId, uri)
                _uiState.update {
                    it.copy(
                        userMessage = if (success) "Database exported successfully" else "Export failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun syncSaf() {
        viewModelScope.launch {
            try {
                val success = repository.syncSafDatabase(databaseId)
                _uiState.update {
                    it.copy(
                        userMessage = if (success) "Changes synced to device storage" else "Sync failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Sync failed: ${e.message}") }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
