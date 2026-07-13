package com.nnddrebuild.nndd_re_tv.nico.watch

import com.nnddrebuild.nndd_re_tv.nico.NicoHttpClient
import kotlinx.serialization.Serializable
import kotlin.random.Random

data class SessionResult(val contentUrl: String, val selectedQualityId: String)

@Serializable
private data class AccessRightsRequest(val outputs: List<List<String>>)

@Serializable
private data class AccessRightsResponse(val data: AccessRightsData? = null)

@Serializable
private data class AccessRightsData(val contentUrl: String? = null)

/**
 * DMS(domand)方式でのHLSセッション確立。移植元: NNDD-RE(PC版) src/main/nicovideo/video/WatchSession.ts
 * MVPではDMS方式のみ対応(DMC旧方式・heartbeatは省略)。
 */
class WatchSession(private val http: NicoHttpClient) {

    /**
     * @param preferredVideoId 指定があればその画質のみをリクエストする。未指定時は最高画質(qualityLevel最大)を自動選択する。
     */
    suspend fun ensure(watch: WatchPageInfo, preferredVideoId: String? = null): SessionResult {
        val accessRightKey = watch.domandAccessRightKey
            ?: throw IllegalStateException("この動画はDMS方式のストリーミングに対応していません")

        val availableAudios = watch.domandAudios.filter { it.isAvailable }
        val availableVideos = watch.domandVideos.filter { it.isAvailable }
        if (availableAudios.isEmpty()) throw IllegalStateException("利用可能な音声ストリームがありません")
        if (availableVideos.isEmpty()) throw IllegalStateException("利用可能な映像ストリームがありません")

        val selectedVideos = preferredVideoId?.let { id -> availableVideos.filter { it.id == id } }
            ?.takeIf { it.isNotEmpty() }
            ?: run {
                // 未指定時は最高画質(qualityLevel最大)のみを選択して再生する
                val best = availableVideos.maxBy { it.qualityLevel }
                listOf(best)
            }

        val outputs = selectedVideos.flatMap { v -> availableAudios.map { a -> listOf(v.id, a.id) } }

        // actionTrackIdはwatch API取得時のclient.watchTrackIdを再利用する必要がある(新規生成は不可)
        val actionTrackId = watch.watchTrackId ?: generateActionTrackId()
        val url = "https://nvapi.nicovideo.jp/v1/watch/${watch.videoId}/access-rights/hls" +
            "?actionTrackId=$actionTrackId"
        val response: AccessRightsResponse = http.postJson(
            url,
            AccessRightsRequest(outputs = outputs),
            mapOf(
                "Accept" to "application/json;charset=utf-8",
                "X-Access-Right-Key" to accessRightKey,
                "X-Request-With" to "https://www.nicovideo.jp"
            )
        )
        val contentUrl = response.data?.contentUrl
            ?: throw IllegalStateException("DMS access-rights APIからcontentUrlが取得できません")
        return SessionResult(contentUrl, selectedVideos.first().id)
    }

    private fun generateActionTrackId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val rand = (1..10).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "${rand}_${System.currentTimeMillis()}"
    }
}
