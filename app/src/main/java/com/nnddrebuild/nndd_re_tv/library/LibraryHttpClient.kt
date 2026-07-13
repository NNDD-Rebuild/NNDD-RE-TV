package com.nnddrebuild.nndd_re_tv.library

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * PC版NNDD-REライブラリ共有サーバー向けの薄いHTTPクライアント。
 * 共有サーバーは認証なし(LAN内前提)のため、Cookie管理や特殊ヘッダーは不要。
 */
class LibraryHttpClient {

    val json = Json { ignoreUnknownKeys = true }

    @PublishedApi
    internal val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getText(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GET $url failed: HTTP ${response.code}")
            }
            response.body?.string() ?: ""
        }
    }

    suspend inline fun <reified T> getJson(url: String): T {
        val text = getText(url)
        return json.decodeFromString(text)
    }

    /** 接続テスト用。例外を投げず成否のみ返す。 */
    suspend fun checkHealth(connection: LibraryConnection): Boolean = try {
        getText("${connection.baseUrl()}/health").contains("\"ok\":true")
    } catch (e: Exception) {
        false
    }
}
