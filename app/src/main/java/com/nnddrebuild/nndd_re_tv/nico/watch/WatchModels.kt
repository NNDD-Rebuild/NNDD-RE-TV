package com.nnddrebuild.nndd_re_tv.nico.watch

data class CommentThreadInfo(val id: String, val fork: String, val isActive: Boolean)

data class DomandStreamCandidate(
    val id: String,
    val isAvailable: Boolean,
    /** 数値が大きいほど高品質。 */
    val qualityLevel: Int,
    /** 表示用ラベル(例: "1080p")。heightがあればそれを、無ければidの末尾から推定する。 */
    val label: String
)

data class CommentTarget(val id: String, val fork: String)

data class NvCommentParams(val targets: List<CommentTarget>, val language: String)

/**
 * ニコニコ動画ウォッチページ情報。移植元: NNDD-RE(PC版) src/shared/types/watch.ts
 * MVPではDMS(domand)方式のみ対象、DMC(旧方式)のフィールドは省略。
 */
data class WatchPageInfo(
    val videoId: String,
    val title: String,
    val duration: Int,
    val thumbnailUrl: String,
    val isDownloadable: Boolean,
    val commentThreads: List<CommentThreadInfo>,
    val threadKey: String?,
    val commentServerUrl: String,
    val domandAccessRightKey: String?,
    val domandVideos: List<DomandStreamCandidate>,
    val domandAudios: List<DomandStreamCandidate>,
    val nvCommentParams: NvCommentParams?,
    /** client.watchTrackId。access-rights/hls呼び出し時のactionTrackIdはこれを再利用する必要がある。 */
    val watchTrackId: String?
)
