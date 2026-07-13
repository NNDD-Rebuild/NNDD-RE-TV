package com.nnddrebuild.nndd_re_tv.library

import kotlinx.serialization.Serializable

/**
 * PC版NNDD-REライブラリ共有サーバー(`/api/library`, `/api/video/:id`)のレスポンス。
 * `id`はDB内部の数値主キー、`videoId`はニコニコ動画ID文字列(例: "sm46520689")。
 * stream/thumb/commentsエンドポイントへのアクセスには必ず`videoId`を使うこと(`id`は404になる)。
 */
@Serializable
data class LibraryVideo(
    val id: Int,
    val videoId: String? = null,
    val videoName: String = "",
    val tags: List<String> = emptyList(),
    val duration: Int = 0,
    val thumbUrl: String? = null,
    val folder: String? = null,
    val description: String? = null
)

/** `/api/video/:id/comments` のレスポンス要素。フィールド構成はnico.comment.NnddCommentと一致する。 */
@Serializable
data class LibraryCommentDto(
    val no: Int,
    val vposMs: Long,
    val text: String,
    val size: Int,
    val pos: String,
    val color: Long,
    val strokeColor: Long? = null
)
