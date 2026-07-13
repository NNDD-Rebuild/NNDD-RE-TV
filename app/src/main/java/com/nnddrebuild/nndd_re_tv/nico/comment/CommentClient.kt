package com.nnddrebuild.nndd_re_tv.nico.comment

import com.nnddrebuild.nndd_re_tv.nico.NicoHttpClient
import com.nnddrebuild.nndd_re_tv.nico.watch.WatchPageInfo
import kotlinx.serialization.Serializable

@Serializable
private data class CommentTargetDto(val id: String, val fork: String)

@Serializable
private data class CommentParamsDto(val targets: List<CommentTargetDto>, val language: String)

@Serializable
private data class CommentThreadsRequest(
    val params: CommentParamsDto,
    val threadKey: String,
    val additionals: Map<String, String> = emptyMap()
)

@Serializable
private data class V3CommentResponse(val data: V3CommentData? = null)

@Serializable
private data class V3CommentData(val threads: List<V3Thread> = emptyList())

@Serializable
private data class V3Thread(val fork: String? = null, val comments: List<V3CommentItem> = emptyList())

@Serializable
private data class V3CommentItem(
    val no: Int,
    val vposMs: Long,
    val body: String,
    val commands: List<String>? = null,
    val isPremium: Boolean? = null
)

/**
 * 最新コメントを1リクエストで取得する。移植元: NNDD-RE(PC版) src/main/nicovideo/comment/CommentClient.ts
 * MVPでは再生開始時の1回取得のみ(全コメント遡り取得は省略)。
 */
class CommentClient(private val http: NicoHttpClient) {

    suspend fun fetchComments(watch: WatchPageInfo): List<NnddComment> {
        val threadKey = watch.threadKey
            ?: throw IllegalStateException("threadKeyが取得できませんでした")
        if (watch.commentThreads.isEmpty()) {
            throw IllegalStateException("コメントスレッドが見つかりません")
        }

        val targets = watch.nvCommentParams?.targets?.map { CommentTargetDto(it.id, it.fork) }
            ?: watch.commentThreads.filter { it.isActive }.map { CommentTargetDto(it.id, normalizeFork(it.fork)) }
        val language = watch.nvCommentParams?.language ?: "ja-jp"

        val url = watch.commentServerUrl.trimEnd('/') + "/v1/threads"
        val response: V3CommentResponse = http.postJson(
            url,
            CommentThreadsRequest(CommentParamsDto(targets, language), threadKey)
        )

        val out = mutableListOf<NnddComment>()
        for (t in response.data?.threads ?: emptyList()) {
            val fork = t.fork ?: "main"
            for (c in t.comments) {
                out += toNnddComment(c, fork)
            }
        }
        return out
    }

    private fun normalizeFork(fork: String): String = when (fork) {
        "0" -> "main"
        "1" -> "owner"
        "2" -> "easy"
        else -> fork
    }

    private fun toNnddComment(c: V3CommentItem, @Suppress("UNUSED_PARAMETER") fork: String): NnddComment {
        val isPremium = c.isPremium ?: false
        val cmd = CommentCommandParser.parse(c.commands, isPremium)
        return NnddComment(
            no = c.no,
            vposMs = c.vposMs,
            text = c.body,
            size = cmd.size,
            pos = cmd.position,
            color = cmd.color,
            strokeColor = cmd.strokeColor
        )
    }
}
