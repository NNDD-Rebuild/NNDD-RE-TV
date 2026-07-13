package com.nnddrebuild.nndd_re_tv.nico.watch

import com.nnddrebuild.nndd_re_tv.nico.NicoCookieJar
import com.nnddrebuild.nndd_re_tv.nico.NicoHttpClient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import java.net.URLEncoder
import kotlin.random.Random

/**
 * 動画IDからウォッチ情報を取得する。移植元: NNDD-RE(PC版) src/main/nicovideo/watch/WatchInfoHandler.ts
 * MVPではJSON API(v3/v3_guest)のみ、HTMLスクレイピングへのフォールバックは省略。
 */
class WatchInfoHandler(
    private val http: NicoHttpClient,
    private val cookieJar: NicoCookieJar
) {
    suspend fun fetchWatchInfo(videoId: String): WatchPageInfo {
        val loggedIn = cookieJar.hasLoginCookie()
        return try {
            fetchViaJsonApi(videoId, loggedIn)
        } catch (e: Exception) {
            if (loggedIn) fetchViaJsonApi(videoId, false) else throw e
        }
    }

    private suspend fun fetchViaJsonApi(videoId: String, loggedIn: Boolean): WatchPageInfo {
        val endpoint = if (loggedIn) "v3" else "v3_guest"
        val url = "https://www.nicovideo.jp/api/watch/$endpoint/${URLEncoder.encode(videoId, "UTF-8")}" +
            "?actionTrackId=${generateActionTrackId()}"
        val json: JsonElement = http.getJson(
            url,
            mapOf(
                "X-Frontend-Id" to "6",
                "X-Frontend-Version" to "0",
                "X-Niconico-Language" to "ja-jp",
                "X-Request-With" to "https://www.nicovideo.jp"
            )
        )
        val dataField = json.jsonObject["data"]
            ?: throw IllegalStateException("watch $endpoint API: data field missing")
        return WatchPageParser.parseApiData(dataField)
    }

    private fun generateActionTrackId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val rand = (1..10).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "${rand}_${System.currentTimeMillis()}"
    }
}
