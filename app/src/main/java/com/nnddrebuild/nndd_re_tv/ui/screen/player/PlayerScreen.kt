package com.nnddrebuild.nndd_re_tv.ui.screen.player

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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.nnddrebuild.nndd_re_tv.nico.NicoApiConstants
import com.nnddrebuild.nndd_re_tv.nico.comment.NnddComment
import com.nnddrebuild.nndd_re_tv.nico.watch.DomandStreamCandidate
import com.nnddrebuild.nndd_re_tv.playback.PlaybackCommand
import com.nnddrebuild.nndd_re_tv.playback.PlaybackCommandBus
import com.nnddrebuild.nndd_re_tv.playback.PlaybackState
import com.nnddrebuild.nndd_re_tv.playback.PlaybackStateBus
import com.nnddrebuild.nndd_re_tv.playback.QualityOption
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(viewModel: PlayerViewModel, videoId: String, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(videoId) {
        viewModel.load(videoId)
    }

    // ブラウザの画質プルダウンからの変更要求はViewModel層で処理する(access-rights APIの再取得が必要なため)
    LaunchedEffect(Unit) {
        PlaybackCommandBus.commands.collect { command ->
            if (command is PlaybackCommand.ChangeQuality) {
                viewModel.changeQuality(command.qualityId)
            }
        }
    }

    when (val state = uiState) {
        is PlayerUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("読み込み中…")
            }
        }

        is PlayerUiState.Error -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("再生に失敗しました: ${state.message}", fontSize = 14.sp)
            }
        }

        is PlayerUiState.Ready -> {
            VideoPlayerContent(
                videoId = videoId,
                title = state.title,
                streamUrl = state.streamUrl,
                comments = state.comments,
                cookieHeader = state.cookieHeader,
                qualities = state.qualities,
                currentQualityId = state.currentQualityId,
                resumePositionMs = state.resumePositionMs,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun VideoPlayerContent(
    videoId: String,
    title: String,
    streamUrl: String,
    comments: List<NnddComment>,
    cookieHeader: String,
    qualities: List<DomandStreamCandidate>,
    currentQualityId: String,
    resumePositionMs: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        // ニコニコのHLS配信URLはReferer/UA/ログインCookie必須のため、ExoPlayerのHTTPデータソースに明示的に付与する
        // (移植元PC版はNicoHttp.fetchが全リクエストへ自動でこれらを付与している)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(NicoApiConstants.defaultHeaders() + mapOf("Cookie" to cookieHeader))
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory))
            .build()
    }
    var positionMs by remember { mutableLongStateOf(0L) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // streamUrlが変わったら(初回ロード、または画質変更)メディアを差し替える。画質変更時はresumePositionMsへシークする。
    LaunchedEffect(streamUrl) {
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
        if (resumePositionMs > 0) {
            exoPlayer.seekTo(resumePositionMs)
        }
        exoPlayer.playWhenReady = true
    }

    // ブラウザの操作パネル(シークバー等)からのコマンドを反映する
    LaunchedEffect(exoPlayer) {
        PlaybackCommandBus.commands.collect { command ->
            when (command) {
                is PlaybackCommand.Pause -> exoPlayer.playWhenReady = false
                is PlaybackCommand.Resume -> exoPlayer.playWhenReady = true
                is PlaybackCommand.SeekTo -> exoPlayer.seekTo(command.positionMs.coerceAtLeast(0))
                is PlaybackCommand.Play -> Unit // 動画切り替えはNavGraph側で処理済み
                is PlaybackCommand.ChangeQuality -> Unit // PlayerScreen側で処理済み
                is PlaybackCommand.PlayLibraryVideo -> Unit // ライブラリ動画への遷移はNavGraph側で処理する
            }
        }
    }

    // 再生状態をブラウザの操作パネルへポーリング公開する
    LaunchedEffect(exoPlayer, qualities, currentQualityId) {
        val qualityOptions = qualities
            .sortedByDescending { it.qualityLevel }
            .map { QualityOption(it.id, it.label) }
        while (true) {
            positionMs = exoPlayer.currentPosition
            val durationMs = exoPlayer.duration.coerceAtLeast(0)
            PlaybackStateBus.update(
                PlaybackState(
                    videoId = videoId,
                    title = title,
                    isPlaying = exoPlayer.playWhenReady,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    qualities = qualityOptions,
                    currentQualityId = currentQualityId
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
