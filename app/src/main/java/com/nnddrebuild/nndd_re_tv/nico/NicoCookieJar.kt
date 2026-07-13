package com.nnddrebuild.nndd_re_tv.nico

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * ニコニコ側Cookie(user_session等)の保持。
 * 移植元: NNDD-RE(PC版) src/main/nicovideo/auth/CookieStore.ts
 * MVP時点ではメモリ内保持のみ。永続化はログイン機能(Step4)実装時に追加する。
 */
class NicoCookieJar : CookieJar {

    private val allCookies = mutableListOf<Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            allCookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
            allCookies.add(cookie)
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        allCookies.removeAll { it.expiresAt <= now }
        return allCookies.filter { it.matches(url) }
    }

    @Synchronized
    fun hasLoginCookie(): Boolean =
        allCookies.any { it.name == NicoApiConstants.USER_SESSION_COOKIE }

    /** 指定URL向けのCookieヘッダー文字列を組み立てる(ExoPlayer等、CookieJarと連動しないHTTPクライアント向け)。 */
    @Synchronized
    fun cookieHeaderFor(url: HttpUrl): String =
        loadForRequest(url).joinToString("; ") { "${it.name}=${it.value}" }

    @Synchronized
    fun clear() {
        allCookies.clear()
    }

    /** 永続化用にCookie一覧をシリアライズ可能な形へ変換する。 */
    @Synchronized
    fun exportCookies(): List<SerializableCookie> = allCookies.map {
        SerializableCookie(
            name = it.name,
            value = it.value,
            domain = it.domain,
            path = it.path,
            expiresAt = it.expiresAt,
            secure = it.secure,
            httpOnly = it.httpOnly,
            hostOnly = it.hostOnly
        )
    }

    /** 永続化されたCookieを復元する。期限切れのものは読み込まない。 */
    @Synchronized
    fun importCookies(cookies: List<SerializableCookie>) {
        val now = System.currentTimeMillis()
        allCookies.clear()
        for (c in cookies) {
            if (c.expiresAt <= now) continue
            val builder = Cookie.Builder()
                .name(c.name)
                .value(c.value)
                .path(c.path)
                .expiresAt(c.expiresAt)
            if (c.hostOnly) builder.hostOnlyDomain(c.domain) else builder.domain(c.domain)
            if (c.secure) builder.secure()
            if (c.httpOnly) builder.httpOnly()
            allCookies.add(builder.build())
        }
    }
}
