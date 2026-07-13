package com.nnddrebuild.nndd_re_tv.nico.auth

import com.nnddrebuild.nndd_re_tv.nico.NicoApiConstants
import com.nnddrebuild.nndd_re_tv.nico.NicoCookieJar
import com.nnddrebuild.nndd_re_tv.nico.NicoCookieStore
import com.nnddrebuild.nndd_re_tv.nico.NicoHttpClient
import kotlinx.serialization.Serializable

@Serializable
private data class UserInfoResponse(val data: UserInfoData? = null)

@Serializable
private data class UserInfoData(val nickname: String? = null, val user: UserInfoUser? = null)

@Serializable
private data class UserInfoUser(val nickname: String? = null)

/**
 * 認証高レベルAPI。移植元: NNDD-RE(PC版) src/main/nicovideo/auth/AuthManager.ts
 * MVPではパスワード保存による自動再ログインは対象外。ログインCookieはNicoCookieStoreで永続化する。
 */
class AuthManager(
    private val http: NicoHttpClient,
    private val cookieJar: NicoCookieJar,
    private val cookieStore: NicoCookieStore
) {
    private val formLoginClient = NicoFormLoginClient(http, cookieJar)

    /** アプリ起動時に永続化済みCookieを復元する。 */
    suspend fun restoreSession() {
        cookieJar.importCookies(cookieStore.load())
    }

    /** 保存済みCookieの有効性をトップページへのアクセスで確認する。 */
    suspend fun checkLoggedIn(): Boolean {
        if (!cookieJar.hasLoginCookie()) return false
        return try {
            val response = http.getRaw(NicoApiConstants.TOP)
            if (response.statusCode == 200) return true
            !(response.location ?: "").contains("login")
        } catch (e: Exception) {
            false
        }
    }

    suspend fun login(email: String, password: String): FormLoginResult {
        val result = formLoginClient.login(email, password)
        if (result.ok) cookieStore.save(cookieJar.exportCookies())
        return result
    }

    suspend fun completeMfa(mfaSubmitUrl: String, code: String): FormLoginResult {
        val result = formLoginClient.completeMfa(mfaSubmitUrl, code)
        if (result.ok) cookieStore.save(cookieJar.exportCookies())
        return result
    }

    suspend fun logout() {
        try {
            http.getRaw(NicoApiConstants.LOGOUT)
        } catch (e: Exception) {
            // サーバー側ログアウト失敗は無視してCookie破棄を続行する
        }
        cookieJar.clear()
        cookieStore.clear()
    }

    fun isLoggedIn(): Boolean = cookieJar.hasLoginCookie()

    /** ログイン中アカウントのニックネームを取得する。未ログイン時・取得失敗時はnull。 */
    suspend fun fetchNickname(): String? {
        if (!cookieJar.hasLoginCookie()) return null
        return try {
            val res: UserInfoResponse = http.getJson("https://nvapi.nicovideo.jp/v1/users/me")
            res.data?.nickname ?: res.data?.user?.nickname
        } catch (e: Exception) {
            android.util.Log.e("NnddAuthManager", "fetchNickname failed", e)
            null
        }
    }
}
