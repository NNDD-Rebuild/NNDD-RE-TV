package com.nnddrebuild.nndd_re_tv.nico.mylist

import com.nnddrebuild.nndd_re_tv.nico.NicoHttpClient
import kotlinx.serialization.Serializable

@Serializable
data class MyListSummary(val id: String, val name: String, val itemsCount: Int = 0)

@Serializable
data class MyListVideoItem(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val length: Int,
    val viewCount: Int,
    val commentCount: Int,
    val mylistCount: Int
)

@Serializable
private data class AccountMylistsResponse(val data: AccountMylistsData? = null)

@Serializable
private data class AccountMylistsData(val mylists: List<AccountMylistEntry> = emptyList())

@Serializable
private data class AccountMylistEntry(val id: Long, val name: String, val itemsCount: Int? = null)

@Serializable
private data class MylistDetailResponse(val data: MylistDetailData? = null)

@Serializable
private data class MylistDetailData(
    val mylist: MylistDetail? = null,
    val items: List<MylistItemDto>? = null
)

@Serializable
private data class MylistDetail(
    val items: List<MylistItemDto>? = null,
    val totalItemCount: Int? = null
)

@Serializable
private data class MylistItemDto(val watchId: String? = null, val video: MylistVideoDto)

@Serializable
private data class MylistVideoDto(
    val id: String,
    val title: String,
    val duration: Int? = null,
    val thumbnail: MylistThumbDto? = null,
    val count: MylistCountDto? = null
)

@Serializable
private data class MylistThumbDto(val url: String? = null)

@Serializable
private data class MylistCountDto(val view: Int? = null, val comment: Int? = null, val mylist: Int? = null)

/**
 * マイリストAPIクライアント。移植元: NNDD-RE(PC版) src/main/nicovideo/mylist/MyListClient.ts
 * MVPではログイン済みアカウントのマイリストのみ対象(未ログイン時の公開マイリストフォールバックは省略)。
 */
class MyListClient(private val http: NicoHttpClient) {

    suspend fun fetchAccountMylists(): List<MyListSummary> {
        val res: AccountMylistsResponse = http.getJson("https://nvapi.nicovideo.jp/v1/users/me/mylists")
        return res.data?.mylists?.map { MyListSummary(it.id.toString(), it.name, it.itemsCount ?: 0) } ?: emptyList()
    }

    suspend fun fetchMylistItems(mylistId: String, pageSize: Int = 100, page: Int = 1): List<MyListVideoItem> {
        val url = "https://nvapi.nicovideo.jp/v1/users/me/mylists/$mylistId?pageSize=$pageSize&page=$page"
        val res: MylistDetailResponse = http.getJson(url)
        val rawItems = res.data?.mylist?.items ?: res.data?.items ?: emptyList()
        return rawItems.map { toItem(it) }
    }

    suspend fun fetchWatchLater(pageSize: Int = 100): List<MyListVideoItem> {
        val url = "https://nvapi.nicovideo.jp/v1/users/me/watch-later?pageSize=$pageSize&sortKey=addedAt&sortOrder=desc"
        val res: MylistDetailResponse = http.getJson(url)
        val rawItems = res.data?.mylist?.items ?: res.data?.items ?: emptyList()
        return rawItems.map { toItem(it) }
    }

    private fun toItem(dto: MylistItemDto): MyListVideoItem = MyListVideoItem(
        videoId = dto.video.id,
        title = dto.video.title,
        thumbnailUrl = dto.video.thumbnail?.url ?: "",
        length = dto.video.duration ?: 0,
        viewCount = dto.video.count?.view ?: 0,
        commentCount = dto.video.count?.comment ?: 0,
        mylistCount = dto.video.count?.mylist ?: 0
    )
}
