package com.nnddrebuild.nndd_re_tv.ui.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nnddrebuild.nndd_re_tv.library.LibraryClient
import com.nnddrebuild.nndd_re_tv.nico.comment.NnddComment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LibraryPlayerUiState {
    data object Loading : LibraryPlayerUiState
    data class Ready(val title: String, val streamUrl: String, val comments: List<NnddComment>) : LibraryPlayerUiState
    data class Error(val message: String) : LibraryPlayerUiState
}

class LibraryPlayerViewModel(private val libraryClient: LibraryClient) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryPlayerUiState>(LibraryPlayerUiState.Loading)
    val uiState: StateFlow<LibraryPlayerUiState> = _uiState.asStateFlow()

    private var loadedVideoId: String? = null

    fun load(videoId: String) {
        if (loadedVideoId == videoId) return
        loadedVideoId = videoId
        viewModelScope.launch {
            _uiState.value = LibraryPlayerUiState.Loading
            _uiState.value = try {
                val video = libraryClient.fetchVideo(videoId)
                val comments = try {
                    libraryClient.fetchComments(videoId)
                } catch (e: Exception) {
                    emptyList()
                }
                val url = libraryClient.streamUrl(videoId)
                android.util.Log.d("NnddLibraryPlayer", "videoId=$videoId streamUrl=$url comments=${comments.size}")
                LibraryPlayerUiState.Ready(
                    title = video.videoName,
                    streamUrl = url,
                    comments = comments
                )
            } catch (e: Exception) {
                android.util.Log.e("NnddLibraryPlayer", "load failed videoId=$videoId", e)
                LibraryPlayerUiState.Error(e.message ?: "動画情報の取得に失敗しました")
            }
        }
    }

    class Factory(private val libraryClient: LibraryClient) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryPlayerViewModel(libraryClient) as T
    }
}
