package com.nnddrebuild.nndd_re_tv.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class QualityOption(val id: String, val label: String)

/** TV側の現在の再生状態。ブラウザの操作パネル(シークバー等)向けにポーリングで公開する。 */
@Serializable
data class PlaybackState(
    val videoId: String? = null,
    val title: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val qualities: List<QualityOption> = emptyList(),
    val currentQualityId: String? = null
)

object PlaybackStateBus {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    fun update(newState: PlaybackState) {
        _state.value = newState
    }
}
