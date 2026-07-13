package com.nnddrebuild.nndd_re_tv.playback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** ブラウザからの再生コマンドをTV UIへ伝える単方向バス。 */
sealed interface PlaybackCommand {
    data class Play(val videoId: String) : PlaybackCommand
    /** PC版NNDD-REライブラリ共有サーバーの動画をTVで再生する(ニコニコ動画のPlayとは別ルートへ遷移する)。 */
    data class PlayLibraryVideo(val videoId: String) : PlaybackCommand
    data object Pause : PlaybackCommand
    data object Resume : PlaybackCommand
    data class SeekTo(val positionMs: Long) : PlaybackCommand
    data class ChangeQuality(val qualityId: String) : PlaybackCommand
}

object PlaybackCommandBus {
    private val _commands = MutableSharedFlow<PlaybackCommand>(extraBufferCapacity = 8)
    val commands: SharedFlow<PlaybackCommand> = _commands

    suspend fun emit(command: PlaybackCommand) = _commands.emit(command)
}
