package com.nnddrebuild.nndd_re_tv.ui.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nnddrebuild.nndd_re_tv.library.LibraryClient
import com.nnddrebuild.nndd_re_tv.library.LibraryConnectionStore
import com.nnddrebuild.nndd_re_tv.library.LibraryVideo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object NotConfigured : LibraryUiState
    data class Success(val items: List<LibraryVideo>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

class LibraryViewModel(
    private val libraryClient: LibraryClient,
    private val connectionStore: LibraryConnectionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            connectionStore.connectionFlow.collect { connection ->
                if (connection == null) {
                    _uiState.value = LibraryUiState.NotConfigured
                } else {
                    load()
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            _uiState.value = try {
                LibraryUiState.Success(libraryClient.fetchLibrary())
            } catch (e: Exception) {
                LibraryUiState.Error(e.message ?: "ライブラリの取得に失敗しました")
            }
        }
    }

    class Factory(
        private val libraryClient: LibraryClient,
        private val connectionStore: LibraryConnectionStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryViewModel(libraryClient, connectionStore) as T
    }
}
