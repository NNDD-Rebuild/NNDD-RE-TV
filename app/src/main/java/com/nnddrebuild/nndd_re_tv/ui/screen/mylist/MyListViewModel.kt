package com.nnddrebuild.nndd_re_tv.ui.screen.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nnddrebuild.nndd_re_tv.nico.mylist.MyListClient
import com.nnddrebuild.nndd_re_tv.nico.mylist.MyListSummary
import com.nnddrebuild.nndd_re_tv.nico.mylist.MyListVideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MyListUiState {
    data object Loading : MyListUiState
    data class ListSuccess(val mylists: List<MyListSummary>) : MyListUiState
    data class ItemsSuccess(val mylistName: String, val items: List<MyListVideoItem>) : MyListUiState
    data class Error(val message: String) : MyListUiState
}

class MyListViewModel(private val myListClient: MyListClient) : ViewModel() {

    private val _uiState = MutableStateFlow<MyListUiState>(MyListUiState.Loading)
    val uiState: StateFlow<MyListUiState> = _uiState.asStateFlow()

    init {
        loadMylists()
    }

    fun loadMylists() {
        viewModelScope.launch {
            _uiState.value = MyListUiState.Loading
            _uiState.value = try {
                MyListUiState.ListSuccess(myListClient.fetchAccountMylists())
            } catch (e: Exception) {
                MyListUiState.Error(e.message ?: "マイリストの取得に失敗しました(ログインが必要です)")
            }
        }
    }

    fun openMylist(id: String, name: String) {
        viewModelScope.launch {
            _uiState.value = MyListUiState.Loading
            _uiState.value = try {
                MyListUiState.ItemsSuccess(name, myListClient.fetchMylistItems(id))
            } catch (e: Exception) {
                MyListUiState.Error(e.message ?: "マイリストの取得に失敗しました")
            }
        }
    }

    class Factory(private val myListClient: MyListClient) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MyListViewModel(myListClient) as T
    }
}
