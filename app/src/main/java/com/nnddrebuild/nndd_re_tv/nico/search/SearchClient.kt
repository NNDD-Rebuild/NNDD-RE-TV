package com.nnddrebuild.nndd_re_tv.nico.search

import com.nnddrebuild.nndd_re_tv.nico.NicoHttpClient
import kotlinx.serialization.Serializable
import java.net.URLEncoder

@Serializable
data class SearchResultItem(
    val videoId: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val length: Int,
    val viewCount: Int,
    val commentCount: Int,
    val mylistCount: Int,
    val likeCount: Int
)

@Serializable
private data class SnapshotV2Response(val data: List<SnapshotV2Item> = emptyList())

@Serializable
private data class SnapshotV2Item(
    val contentId: String,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val lengthSeconds: Int? = null,
    val viewCounter: Int? = null,
    val commentCounter: Int? = null,
    val mylistCounter: Int? = null,
    val likeCounter: Int? = null
)

/**
 * スナップショット検索API v2クライアント。
 * 移植元: NNDD-RE(PC版) src/main/nicovideo/search/SearchClient.ts
 * MVPではキーワード検索のみ(動画ID直接検索は省略)。
 */
class SearchClient(private val http: NicoHttpClient) {

    suspend fun search(word: String, offset: Int = 0, limit: Int = 32): List<SearchResultItem> {
        val url = buildUrl(word, offset, limit)
        val res: SnapshotV2Response = http.getJson(url)
        return res.data.map {
            SearchResultItem(
                videoId = it.contentId,
                title = it.title,
                description = it.description ?: "",
                thumbnailUrl = it.thumbnailUrl ?: "",
                length = it.lengthSeconds ?: 0,
                viewCount = it.viewCounter ?: 0,
                commentCount = it.commentCounter ?: 0,
                mylistCount = it.mylistCounter ?: 0,
                likeCount = it.likeCounter ?: 0
            )
        }
    }

    private fun buildUrl(word: String, offset: Int, limit: Int): String {
        val encoded = URLEncoder.encode(word, "UTF-8")
        return "https://snapshot.search.nicovideo.jp/api/v2/snapshot/video/contents/search" +
            "?q=$encoded&targets=title,description,tags" +
            "&fields=contentId,title,description,thumbnailUrl,lengthSeconds,viewCounter,commentCounter,mylistCounter,likeCounter,startTime,tags,channelId" +
            "&_sort=-viewCounter&_offset=$offset&_limit=$limit&_context=nndd-re-tv"
    }
}
