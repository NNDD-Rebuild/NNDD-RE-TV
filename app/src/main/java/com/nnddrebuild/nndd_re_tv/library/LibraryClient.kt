package com.nnddrebuild.nndd_re_tv.library

import com.nnddrebuild.nndd_re_tv.nico.comment.NnddComment

/**
 * PC版NNDD-REライブラリ共有サーバーの高レベルAPI。
 * 移植元(参考): NNDD-RE-android data/repository/NnddRepository.kt
 */
class LibraryClient(private val http: LibraryHttpClient) {

    @Volatile
    private var connection: LibraryConnection? = null

    fun setConnection(connection: LibraryConnection?) {
        this.connection = connection
    }

    fun hasConnection(): Boolean = connection != null

    suspend fun fetchLibrary(): List<LibraryVideo> {
        val conn = requireConnection()
        return http.getJson("${conn.baseUrl()}/api/library")
    }

    suspend fun fetchVideo(videoId: String): LibraryVideo {
        val conn = requireConnection()
        return http.getJson("${conn.baseUrl()}/api/video/$videoId")
    }

    suspend fun fetchComments(videoId: String): List<NnddComment> {
        val conn = requireConnection()
        val dtos: List<LibraryCommentDto> = http.getJson("${conn.baseUrl()}/api/video/$videoId/comments")
        return dtos.map {
            NnddComment(
                no = it.no,
                vposMs = it.vposMs,
                text = it.text,
                size = it.size,
                pos = it.pos,
                color = it.color,
                strokeColor = it.strokeColor
            )
        }
    }

    fun streamUrl(videoId: String): String = "${requireConnection().baseUrl()}/api/video/$videoId/stream"

    fun thumbUrl(videoId: String): String = "${requireConnection().baseUrl()}/api/video/$videoId/thumb"

    private fun requireConnection(): LibraryConnection =
        connection ?: throw IllegalStateException("ライブラリサーバーが未設定です")
}
