package com.nnddrebuild.nndd_re_tv.ui.screen.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nnddrebuild.nndd_re_tv.nico.ranking.RankingClient
import com.nnddrebuild.nndd_re_tv.nico.ranking.RankingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RankingUiState {
    data object Loading : RankingUiState
    data class Success(val items: List<RankingItem>) : RankingUiState
    data class Error(val message: String) : RankingUiState
}

class RankingViewModel(private val rankingClient: RankingClient) : ViewModel() {

    private val _uiState = MutableStateFlow<RankingUiState>(RankingUiState.Loading)
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = RankingUiState.Loading
            _uiState.value = try {
                RankingUiState.Success(rankingClient.fetch())
            } catch (e: Exception) {
                RankingUiState.Error(e.message ?: "ランキングの取得に失敗しました")
            }
        }
    }

    class Factory(private val rankingClient: RankingClient) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RankingViewModel(rankingClient) as T
    }
}
