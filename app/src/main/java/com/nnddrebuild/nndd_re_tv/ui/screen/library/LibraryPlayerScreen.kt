package com.nnddrebuild.nndd_re_tv.ui.screen.library

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.nnddrebuild.nndd_re_tv.nico.comment.NnddComment
import com.nnddrebuild.nndd_re_tv.playback.PlaybackCommand
import com.nnddrebuild.nndd_re_tv.playback.PlaybackCommandBus
import com.nnddrebuild.nndd_re_tv.playback.PlaybackState
import com.nnddrebuild.nndd_re_tv.playback.PlaybackStateBus
import com.nnddrebuild.nndd_re_tv.ui.screen.player.CommentOverlay
import kotlinx.coroutines.delay

/**
 * PC版NNDD-REライブラリ共有サーバーの動画再生画面。
 * ニコニコ動画再生(player.PlayerScreen)とは独立した実装。共有サーバーは認証不要のため
 * Cookie/カスタムヘッダーの付与は不要(ExoPlayerへプレーンにMediaItemをセットするだけでよい)。
 */
@Composable
fun LibraryPlayerScreen(viewModel: LibraryPlayerViewModel, videoId: String, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(videoId) {
        viewModel.load(videoId)
    }

    when (val state = uiState) {
        is LibraryPlayerUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("読み込み中…")
            }
        }

        is LibraryPlayerUiState.Error -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("再生に失敗しました: ${state.message}", fontSize = 14.sp)
            }
        }

        is LibraryPlayerUiState.Ready -> {
            LibraryVideoPlayerContent(
                videoId = videoId,
                title = state.title,
                streamUrl = state.streamUrl,
                comments = state.comments,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun LibraryVideoPlayerContent(
    videoId: String,
    title: String,
    streamUrl: String,
    comments: List<NnddComment>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var positionMs by remember { mutableLongStateOf(0L) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(streamUrl) {
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // ブラウザの操作パネル(player.html)からのコマンドを反映する(画質切り替え・動画切り替えコマンドはここでは扱わない)
    LaunchedEffect(exoPlayer) {
        PlaybackCommandBus.commands.collect { command ->
            when (command) {
                is PlaybackCommand.Pause -> exoPlayer.playWhenReady = false
                is PlaybackCommand.Resume -> exoPlayer.playWhenReady = true
                is PlaybackCommand.SeekTo -> exoPlayer.seekTo(command.positionMs.coerceAtLeast(0))
                else -> Unit
            }
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            positionMs = exoPlayer.currentPosition
            val durationMs = exoPlayer.duration.coerceAtLeast(0)
            PlaybackStateBus.update(
                PlaybackState(
                    videoId = videoId,
                    title = title,
                    isPlaying = exoPlayer.playWhenReady,
                    positionMs = positionMs,
                    durationMs = durationMs
                )
            )
            delay(200)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            PlaybackStateBus.update(PlaybackState())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter -> {
                        exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                        true
                    }

                    Key.DirectionLeft -> {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                        true
                    }

                    Key.DirectionRight -> {
                        exoPlayer.seekTo(exoPlayer.currentPosition + 10_000)
                        true
                    }

                    Key.Back -> {
                        onBack()
                        true
                    }

                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        CommentOverlay(
            comments = comments,
            currentPositionMs = positionMs,
            modifier = Modifier.fillMaxSize()
        )
    }
}
