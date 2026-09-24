package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.repository.DatabaseRepository

class ViewModelFactory(
    private val repository: DatabaseRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DatabaseListViewModel::class.java) -> {
                DatabaseListViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SqlConsoleViewModel::class.java) -> {
                SqlConsoleViewModel(null, repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
