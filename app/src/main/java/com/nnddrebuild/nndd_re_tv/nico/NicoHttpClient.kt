package com.nnddrebuild.nndd_re_tv.nico

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.serializer
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** リダイレクトを手動追跡するためのstatus/Locationヘッダー付き生レスポンス。 */
data class NicoHttpResponse(
    val statusCode: Int,
    val location: String?,
    val bodyText: String
)

/**
 * ニコニコAPI向けHTTPクライアント。
 * 移植元: NNDD-RE(PC版) src/main/nicovideo/NicoHttp.ts
 * 必須ヘッダーを常に付与し、CookieJarと連動してCookie送受信を自動化する。
 */
class NicoHttpClient(cookieJar: NicoCookieJar) {

    val json = Json { ignoreUnknownKeys = true }

    @PublishedApi
    internal val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getText(url: String, extraHeaders: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val request = buildRequest(url, extraHeaders)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("GET $url failed: HTTP ${response.code}")
                }
                response.body?.string() ?: ""
            }
        }

    suspend inline fun <reified T> getJson(
        url: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): T {
        val text = getText(url, mapOf("Accept" to "application/json, */*;q=0.8") + extraHeaders)
        return json.decodeFromString(text)
    }

    /** JSONボディをPOSTしてJSONをデコードする。 */
    suspend inline fun <reified TReq, reified TRes> postJson(
        url: String,
        body: TReq,
        extraHeaders: Map<String, String> = emptyMap()
    ): TRes = withContext(Dispatchers.IO) {
        val bodyJson = json.encodeToString(serializer<TReq>(), body)
        val builder = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
        for ((k, v) in NicoApiConstants.defaultHeaders()) builder.addHeader(k, v)
        builder.header("Accept", "application/json, */*;q=0.8")
        for ((k, v) in extraHeaders) builder.header(k, v)
        val text = client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string()?.take(500) ?: ""
                throw IOException("POST $url failed: HTTP ${response.code} body=$bodyJson resp=$errBody")
            }
            response.body?.string() ?: ""
        }
        json.decodeFromString(text)
    }

    /** 302等をそのままstatus/Locationとして返す(手動リダイレクト追跡用)。GET。 */
    suspend fun getRaw(url: String, extraHeaders: Map<String, String> = emptyMap()): NicoHttpResponse =
        withContext(Dispatchers.IO) {
            val request = buildRequest(url, extraHeaders)
            client.newCall(request).execute().use { response ->
                NicoHttpResponse(
                    statusCode = response.code,
                    location = response.header("Location"),
                    bodyText = response.body?.string() ?: ""
                )
            }
        }

    /** x-www-form-urlencoded POST。302等をそのままstatus/Locationとして返す。 */
    suspend fun postForm(
        url: String,
        formBody: Map<String, String>,
        extraHeaders: Map<String, String> = emptyMap()
    ): NicoHttpResponse = withContext(Dispatchers.IO) {
        val bodyBuilder = FormBody.Builder()
        for ((k, v) in formBody) bodyBuilder.add(k, v)
        val builder = Request.Builder().url(url).post(bodyBuilder.build())
        for ((k, v) in NicoApiConstants.defaultHeaders()) builder.addHeader(k, v)
        for ((k, v) in extraHeaders) builder.addHeader(k, v)
        client.newCall(builder.build()).execute().use { response ->
            NicoHttpResponse(
                statusCode = response.code,
                location = response.header("Location"),
                bodyText = response.body?.string() ?: ""
            )
        }
    }

    fun buildRequest(url: String, extraHeaders: Map<String, String>): Request {
        val builder = Request.Builder().url(url)
        for ((k, v) in NicoApiConstants.defaultHeaders()) builder.addHeader(k, v)
        for ((k, v) in extraHeaders) builder.addHeader(k, v)
        return builder.build()
    }
}
