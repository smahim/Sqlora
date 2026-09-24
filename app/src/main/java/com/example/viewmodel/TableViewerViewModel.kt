package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ColumnInfo
import com.example.data.model.ForeignKeyInfo
import com.example.data.model.IndexInfo
import com.example.data.model.QueryResult
import com.example.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TableViewerUiState(
    val tableName: String = "",
    val columns: List<ColumnInfo> = emptyList(),
    val foreignKeys: List<ForeignKeyInfo> = emptyList(),
    val indexes: List<IndexInfo> = emptyList(),
    val queryResult: QueryResult? = null,
    val isLoading: Boolean = true,
    val pageLimit: Int = 50,
    val pageOffset: Int = 0,
    val filterText: String = "",
    val error: String? = null
) {
    val displayedRows: List<List<String?>>
        get() {
            val rows = queryResult?.rows ?: emptyList()
            if (filterText.isBlank()) return rows
            return rows.filter { row ->
                row.any { cell -> cell?.contains(filterText, ignoreCase = true) == true }
            }
        }
}

class TableViewerViewModel(
    private val databaseId: Long,
    private val tableName: String,
    private val repository: DatabaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TableViewerUiState(
            tableName = tableName,
            isLoading = true
        )
    )
    val uiState: StateFlow<TableViewerUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val cols = repository.getTableColumns(databaseId, tableName)
                val meta = try { repository.getDatabaseMetadata(databaseId) } catch (_: Exception) { null }
                val tableMeta = meta?.tables?.find { it.name == tableName }
                val currentLimit = _uiState.value.pageLimit
                val currentOffset = _uiState.value.pageOffset
                val result = repository.getTableRows(databaseId, tableName, currentLimit, currentOffset)
                _uiState.update {
                    it.copy(
                        columns = cols,
                        foreignKeys = tableMeta?.foreignKeys ?: emptyList(),
                        indexes = tableMeta?.indexes ?: emptyList(),
                        queryResult = result,
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

    fun nextPage() {
        _uiState.update { it.copy(pageOffset = it.pageOffset + it.pageLimit) }
        loadData()
    }

    fun prevPage() {
        val currentOffset = _uiState.value.pageOffset
        val limit = _uiState.value.pageLimit
        if (currentOffset >= limit) {
            _uiState.update { it.copy(pageOffset = currentOffset - limit) }
            loadData()
        }
    }

    fun setPageLimit(limit: Int) {
        _uiState.update { it.copy(pageLimit = limit, pageOffset = 0) }
        loadData()
    }

    fun setFilterText(filter: String) {
        _uiState.update { it.copy(filterText = filter) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
