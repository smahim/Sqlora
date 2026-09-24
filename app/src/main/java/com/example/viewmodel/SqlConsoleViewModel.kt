package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DatabaseItem
import com.example.data.model.QueryHistoryItem
import com.example.data.model.QueryResult
import com.example.data.model.SavedQueryItem
import com.example.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class SqlTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sqlText: String = "",
    val queryResult: QueryResult? = null,
    val isExecuting: Boolean = false
)

data class SqlConsoleUiState(
    val databases: List<DatabaseItem> = emptyList(),
    val selectedDatabaseId: Long? = null,
    val tabs: List<SqlTab> = listOf(
        SqlTab(
            id = "tab_1",
            title = "Query 1",
            sqlText = "SELECT * FROM sqlite_master WHERE type='table';"
        )
    ),
    val activeTabId: String = "tab_1",
    val activeBottomTab: Int = 0, // 0: Results, 1: Chart, 2: History, 3: Saved Queries
    val errorMessage: String? = null
) {
    val currentTab: SqlTab?
        get() = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

    val sqlText: String
        get() = currentTab?.sqlText ?: ""

    val queryResult: QueryResult?
        get() = currentTab?.queryResult

    val isExecuting: Boolean
        get() = currentTab?.isExecuting == true
}

class SqlConsoleViewModel(
    private val initialDatabaseId: Long?,
    private val repository: DatabaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SqlConsoleUiState(
            selectedDatabaseId = initialDatabaseId
        )
    )
    val uiState: StateFlow<SqlConsoleUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<QueryHistoryItem>>(emptyList())
    val history: StateFlow<List<QueryHistoryItem>> = _history.asStateFlow()

    val savedQueries: StateFlow<List<SavedQueryItem>> = repository.getSavedQueries().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.allDatabases.collect { dbs ->
                _uiState.update { current ->
                    val selectedId = current.selectedDatabaseId ?: dbs.firstOrNull()?.id
                    current.copy(
                        databases = dbs,
                        selectedDatabaseId = selectedId
                    )
                }
                val currentDbId = _uiState.value.selectedDatabaseId
                if (currentDbId != null) {
                    observeHistoryForDb(currentDbId)
                }
            }
        }
    }

    fun selectDatabase(id: Long) {
        _uiState.update { it.copy(selectedDatabaseId = id) }
        observeHistoryForDb(id)
    }

    private fun observeHistoryForDb(id: Long) {
        viewModelScope.launch {
            repository.getHistoryForDatabase(id).collect { list ->
                _history.value = list
            }
        }
    }

    // --- Tab Management ---

    fun addNewTab(initialSql: String = "SELECT * FROM sqlite_master WHERE type='table';", title: String? = null) {
        _uiState.update { current ->
            val nextNumber = current.tabs.size + 1
            val newTitle = title ?: "Query $nextNumber"
            val newTab = SqlTab(
                title = newTitle,
                sqlText = initialSql
            )
            current.copy(
                tabs = current.tabs + newTab,
                activeTabId = newTab.id
            )
        }
    }

    fun selectTab(tabId: String) {
        _uiState.update { it.copy(activeTabId = tabId) }
    }

    fun closeTab(tabId: String) {
        _uiState.update { current ->
            if (current.tabs.size <= 1) {
                // If closing last tab, reset to an empty query tab
                val fallbackTab = SqlTab(id = "tab_1", title = "Query 1", sqlText = "")
                return@update current.copy(tabs = listOf(fallbackTab), activeTabId = fallbackTab.id)
            }
            val newTabs = current.tabs.filter { it.id != tabId }
            val newActiveId = if (current.activeTabId == tabId) {
                newTabs.lastOrNull()?.id ?: ""
            } else {
                current.activeTabId
            }
            current.copy(tabs = newTabs, activeTabId = newActiveId)
        }
    }

    fun renameTab(tabId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        _uiState.update { current ->
            val updated = current.tabs.map { tab ->
                if (tab.id == tabId) tab.copy(title = newTitle.trim()) else tab
            }
            current.copy(tabs = updated)
        }
    }

    fun setSqlText(sql: String) {
        _uiState.update { current ->
            val updated = current.tabs.map { tab ->
                if (tab.id == current.activeTabId) tab.copy(sqlText = sql) else tab
            }
            current.copy(tabs = updated)
        }
    }

    fun insertKeyword(keyword: String) {
        val current = _uiState.value.sqlText
        val newSql = if (current.isBlank()) {
            "$keyword "
        } else {
            val space = if (current.endsWith(" ")) "" else " "
            "$current$space$keyword "
        }
        setSqlText(newSql)
    }

    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeBottomTab = tab) }
    }

    fun executeSql() {
        val dbId = _uiState.value.selectedDatabaseId
        if (dbId == null) {
            _uiState.update { it.copy(errorMessage = "Please select a database first") }
            return
        }
        val currentTabId = _uiState.value.activeTabId
        val sql = _uiState.value.sqlText.trim()
        if (sql.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a SQL query") }
            return
        }

        viewModelScope.launch {
            _uiState.update { current ->
                val updated = current.tabs.map { tab ->
                    if (tab.id == currentTabId) tab.copy(isExecuting = true) else tab
                }
                current.copy(tabs = updated, errorMessage = null, activeBottomTab = 0)
            }

            try {
                val result = repository.executeSql(dbId, sql)
                _uiState.update { current ->
                    val updated = current.tabs.map { tab ->
                        if (tab.id == currentTabId) tab.copy(queryResult = result, isExecuting = false) else tab
                    }
                    current.copy(tabs = updated)
                }
            } catch (e: Exception) {
                _uiState.update { current ->
                    val errResult = QueryResult(errorMessage = e.message ?: "Execution error")
                    val updated = current.tabs.map { tab ->
                        if (tab.id == currentTabId) tab.copy(queryResult = errResult, isExecuting = false) else tab
                    }
                    current.copy(tabs = updated)
                }
            }
        }
    }

    fun bookmarkCurrentQuery(title: String) {
        val sql = _uiState.value.sqlText.trim()
        if (sql.isBlank() || title.isBlank()) return
        viewModelScope.launch {
            repository.saveQuery(title, sql, _uiState.value.selectedDatabaseId)
        }
    }

    fun deleteSavedQuery(queryId: Long) {
        viewModelScope.launch {
            repository.deleteSavedQuery(queryId)
        }
    }

    fun deleteHistoryItem(historyId: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(historyId)
        }
    }

    fun clearSql() {
        setSqlText("")
    }

    fun clearHistory() {
        val dbId = _uiState.value.selectedDatabaseId ?: return
        viewModelScope.launch {
            repository.clearHistory(dbId)
        }
    }

    fun loadQueryIntoNewTab(title: String, sql: String) {
        addNewTab(initialSql = sql, title = title)
    }

    fun loadQueryIntoCurrentTab(sql: String) {
        setSqlText(sql)
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
