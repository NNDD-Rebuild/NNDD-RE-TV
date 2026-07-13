package com.nnddrebuild.nndd_re_tv.ui.screen.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nnddrebuild.nndd_re_tv.nico.NicoCookieJar
import com.nnddrebuild.nndd_re_tv.nico.comment.CommentClient
import com.nnddrebuild.nndd_re_tv.nico.comment.NnddComment
import com.nnddrebuild.nndd_re_tv.nico.watch.DomandStreamCandidate
import com.nnddrebuild.nndd_re_tv.nico.watch.WatchInfoHandler
import com.nnddrebuild.nndd_re_tv.nico.watch.WatchPageInfo
import com.nnddrebuild.nndd_re_tv.nico.watch.WatchSession
import com.nnddrebuild.nndd_re_tv.playback.PlaybackStateBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Ready(
        val title: String,
        val streamUrl: String,
        val comments: List<NnddComment>,
        val cookieHeader: String,
        val qualities: List<DomandStreamCandidate>,
        val currentQualityId: String,
        val resumePositionMs: Long = 0
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

class PlayerViewModel(
    private val watchInfoHandler: WatchInfoHandler,
    private val watchSession: WatchSession,
    private val commentClient: CommentClient,
    private val cookieJar: NicoCookieJar
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var loadedVideoId: String? = null
    private var currentWatch: WatchPageInfo? = null

    fun load(videoId: String) {
        if (loadedVideoId == videoId) return
        loadedVideoId = videoId
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            _uiState.value = try {
                val watch = watchInfoHandler.fetchWatchInfo(videoId)
                currentWatch = watch
                val session = try {
                    watchSession.ensure(watch)
                } catch (e: Exception) {
                    throw IllegalStateException(
                        "${e.message} | videos=${watch.domandVideos} audios=${watch.domandAudios} " +
                            "accessRightKey=${watch.domandAccessRightKey?.take(20)}"
                    )
                }
                val comments = try {
                    commentClient.fetchComments(watch)
                } catch (e: Exception) {
                    emptyList()
                }
                val cookieHeader = cookieJar.cookieHeaderFor(session.contentUrl.toHttpUrl())
                android.util.Log.d("NnddPlayer", "contentUrl=${session.contentUrl}")
                PlayerUiState.Ready(
                    title = watch.title,
                    streamUrl = session.contentUrl,
                    comments = comments,
                    cookieHeader = cookieHeader,
                    qualities = watch.domandVideos.filter { it.isAvailable },
                    currentQualityId = session.selectedQualityId
                )
            } catch (e: Exception) {
                PlayerUiState.Error(e.message ?: "動画情報の取得に失敗しました")
            }
        }
    }

    /** ブラウザの画質プルダウンからの変更要求。現在の再生位置を保持したまま別画質のcontentUrlへ切り替える。 */
    fun changeQuality(qualityId: String) {
        val watch = currentWatch ?: return
        val current = _uiState.value as? PlayerUiState.Ready ?: return
        if (current.currentQualityId == qualityId) return
        viewModelScope.launch {
            try {
                val resumeMs = PlaybackStateBus.state.value.positionMs
                val session = watchSession.ensure(watch, preferredVideoId = qualityId)
                val cookieHeader = cookieJar.cookieHeaderFor(session.contentUrl.toHttpUrl())
                _uiState.value = current.copy(
                    streamUrl = session.contentUrl,
                    cookieHeader = cookieHeader,
                    currentQualityId = session.selectedQualityId,
                    resumePositionMs = resumeMs
                )
            } catch (e: Exception) {
                android.util.Log.e("NnddPlayer", "changeQuality failed", e)
            }
        }
    }

    class Factory(
        private val watchInfoHandler: WatchInfoHandler,
        private val watchSession: WatchSession,
        private val commentClient: CommentClient,
        private val cookieJar: NicoCookieJar
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlayerViewModel(watchInfoHandler, watchSession, commentClient, cookieJar) as T
    }
}
