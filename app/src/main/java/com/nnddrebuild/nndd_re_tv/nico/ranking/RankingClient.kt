package com.nnddrebuild.nndd_re_tv.nico.ranking

import com.nnddrebuild.nndd_re_tv.nico.NicoHttpClient
import kotlinx.serialization.Serializable

@Serializable
data class RankingItem(
    val rank: Int,
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
private data class NvApiRankingResponse(val data: NvApiRankingData? = null)

@Serializable
private data class NvApiRankingData(val items: List<NvApiRankingVideo> = emptyList())

@Serializable
private data class NvApiRankingVideo(
    val id: String,
    val title: String,
    val duration: Int? = null,
    val thumbnail: NvApiThumbnail? = null,
    val count: NvApiCount? = null,
    val shortDescription: String? = null
)

@Serializable
private data class NvApiThumbnail(
    val url: String? = null,
    val middleUrl: String? = null,
    val largeUrl: String? = null
)

@Serializable
private data class NvApiCount(
    val view: Int? = null,
    val comment: Int? = null,
    val mylist: Int? = null,
    val like: Int? = null
)

/**
 * ランキング取得。移植元: NNDD-RE(PC版) src/main/nicovideo/ranking/RankingClient.ts
 * MVPではnvapi直叩きのみ(PC版にあるRSSフォールバックは省略)。
 */
class RankingClient(private val http: NicoHttpClient) {

    suspend fun fetch(genre: String = "all", term: String = "24h"): List<RankingItem> {
        val url = "https://nvapi.nicovideo.jp/v1/ranking/genre/$genre?term=$term&pageSize=100&page=1"
        val res: NvApiRankingResponse = http.getJson(url)
        val items = res.data?.items ?: emptyList()
        return items.mapIndexed { idx, v ->
            RankingItem(
                rank = idx + 1,
                videoId = v.id,
                title = v.title,
                description = v.shortDescription ?: "",
                thumbnailUrl = v.thumbnail?.middleUrl ?: v.thumbnail?.url ?: "",
                length = v.duration ?: 0,
                viewCount = v.count?.view ?: 0,
                commentCount = v.count?.comment ?: 0,
                mylistCount = v.count?.mylist ?: 0,
                likeCount = v.count?.like ?: 0
            )
        }
    }
}
