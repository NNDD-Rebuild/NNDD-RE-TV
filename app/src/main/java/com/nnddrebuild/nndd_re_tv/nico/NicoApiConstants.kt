package com.nnddrebuild.nndd_re_tv.nico

/**
 * ニコニコ動画APIのエンドポイント・ヘッダー定数。
 * 移植元: NNDD-RE(PC版) src/shared/constants/api.ts
 */
object NicoApiConstants {
    const val WATCH_PAGE = "https://www.nicovideo.jp/watch/"
    const val TOP = "https://www.nicovideo.jp/"
    const val LOGIN_POST = "https://account.nicovideo.jp/api/v1/login?site=niconico&next_url=%2F"
    const val LOGOUT = "https://secure.nicovideo.jp/secure/logout"
    const val DMC_SESSION_BASE = "https://api.dmc.nico/api/sessions/"
    const val COMMENT_THREADS_V3 = "https://nvcomment.nicovideo.jp/v1/threads"
    const val MYLIST_API_BASE = "https://nvapi.nicovideo.jp/v1/users/me/mylists"
    const val PUBLIC_MYLIST_API = "https://nvapi.nicovideo.jp/v2/mylists/"
    const val WATCH_LATER_API = "https://nvapi.nicovideo.jp/v1/users/me/watch-later"
    const val SEARCH_API = "https://snapshot.search.nicovideo.jp/api/v2/snapshot/video/contents/search"
    const val RANKING_RSS = "https://www.nicovideo.jp/ranking/"

    const val USER_SESSION_COOKIE = "user_session"
    const val USER_SESSION_SECURE_COOKIE = "user_session_secure"

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
    private const val REFERER = "https://www.nicovideo.jp/"

    fun defaultHeaders(): Map<String, String> = mapOf(
        "User-Agent" to USER_AGENT,
        "Referer" to REFERER,
        "X-Frontend-Id" to "6",
        "X-Frontend-Version" to "0",
        "X-Client-Os-Type" to "others",
        "X-Niconico-Language" to "ja-jp"
    )
}
