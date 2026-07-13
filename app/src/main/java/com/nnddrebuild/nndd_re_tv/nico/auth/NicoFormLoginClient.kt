package com.nnddrebuild.nndd_re_tv.nico.auth

import com.nnddrebuild.nndd_re_tv.nico.NicoCookieJar
import com.nnddrebuild.nndd_re_tv.nico.NicoHttpClient
import java.net.URL
import java.net.URLDecoder

data class FormLoginResult(
    val ok: Boolean,
    val mfaRequired: Boolean = false,
    val mfaSubmitUrl: String? = null,
    val error: String? = null
)

private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)

private val MFA_TOKEN_RE =
    Regex("""<input\b[^>]*\bname=["']loginBouncerChallengeResponseToken["'][^>]*\bvalue=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val MFA_TOKEN_RE_REV =
    Regex("""<input\b[^>]*\bvalue=["']([^"']+)["'][^>]*\bname=["']loginBouncerChallengeResponseToken["']""", RegexOption.IGNORE_CASE)
private val MFA_FORM_RE =
    Regex("""<form\b[^>]*(?:id|class|action)=["'][^"']*mfa[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE)
private val FORM_RE = Regex("""<form\b[^>]*>""", RegexOption.IGNORE_CASE)
private val ACTION_RE = Regex("""\baction=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

/**
 * ID/Pass によるアプリ内ログインクライアント。
 * 移植元: NNDD-RE(PC版) src/main/nicovideo/auth/NicoFormLoginClient.ts
 *
 * 1) POST https://account.nicovideo.jp/api/v1/login?site=niconico&next_url=/
 *    成功時はSet-Cookieでuser_sessionが付与される。MFA要求時は /mfa への302が返る。
 * 2) POST <mfaSubmitUrl> (MFAページから抽出したloginBouncerChallengeResponseToken同梱)
 */
class NicoFormLoginClient(
    private val http: NicoHttpClient,
    private val cookieJar: NicoCookieJar
) {

    suspend fun login(email: String, password: String): FormLoginResult {
        var currentUrl = LOGIN_POST_URL
        var currentMethod = "POST"
        var currentBody: Map<String, String>? = mapOf("mail_tel" to email, "password" to password)
        var lastLocation = ""
        var mfaUrl: String? = null

        for (hop in 0 until 5) {
            val headers = mapOf(
                "Origin" to "https://account.nicovideo.jp",
                "Referer" to if (hop == 0) "https://account.nicovideo.jp/login" else currentUrl
            )
            val response = if (currentMethod == "POST") {
                http.postForm(currentUrl, currentBody ?: emptyMap(), headers)
            } else {
                http.getRaw(currentUrl, headers)
            }
            lastLocation = response.location ?: ""

            if (cookieJar.hasLoginCookie()) {
                return FormLoginResult(ok = true)
            }

            if (lastLocation.contains("/mfa")) {
                mfaUrl = resolveUrl("https://account.nicovideo.jp", lastLocation)
                break
            }

            if (response.statusCode in REDIRECT_CODES && lastLocation.isNotEmpty()) {
                currentUrl = resolveUrl(currentUrl, lastLocation)
                currentMethod = "GET"
                currentBody = null
                continue
            }

            break
        }

        if (mfaUrl != null) {
            return FormLoginResult(ok = false, mfaRequired = true, mfaSubmitUrl = mfaUrl)
        }

        return FormLoginResult(
            ok = false,
            error = extractErrorMessage(lastLocation)
        )
    }

    suspend fun completeMfa(mfaSubmitUrl: String, code: String): FormLoginResult {
        val html = http.getText(mfaSubmitUrl, mapOf("Accept" to "text/html,application/xhtml+xml"))

        val tokenMatch = MFA_TOKEN_RE.find(html) ?: MFA_TOKEN_RE_REV.find(html)
        val formMatch = MFA_FORM_RE.find(html) ?: FORM_RE.find(html)
        val actionMatch = formMatch?.value?.let { ACTION_RE.find(it) }
        val submitUrl = actionMatch?.groupValues?.get(1)?.let { resolveUrl(mfaSubmitUrl, it) } ?: mfaSubmitUrl

        val body = mutableMapOf("otp" to code)
        tokenMatch?.groupValues?.get(1)?.let { body["loginBouncerChallengeResponseToken"] = it }
        body["is_mfa_trusted_device"] = "true"

        var currentUrl = submitUrl
        var currentMethod = "POST"
        var currentBody: Map<String, String>? = body
        var lastLocation = ""

        for (hop in 0 until 5) {
            val headers = mapOf(
                "Origin" to "https://account.nicovideo.jp",
                "Referer" to if (hop == 0) mfaSubmitUrl else currentUrl
            )
            val response = if (currentMethod == "POST") {
                http.postForm(currentUrl, currentBody ?: emptyMap(), headers)
            } else {
                http.getRaw(currentUrl, headers)
            }
            lastLocation = response.location ?: ""

            if (cookieJar.hasLoginCookie()) {
                return FormLoginResult(ok = true)
            }

            if (response.statusCode in REDIRECT_CODES && lastLocation.isNotEmpty()) {
                currentUrl = resolveUrl(currentUrl, lastLocation)
                currentMethod = "GET"
                currentBody = null
                continue
            }

            break
        }

        return FormLoginResult(ok = false, error = extractErrorMessage(lastLocation, "2段階認証コードが正しくありません"))
    }

    private fun extractErrorMessage(
        location: String,
        fallback: String = "メールアドレスまたはパスワードが正しくありません"
    ): String {
        val marker = "message="
        val idx = location.indexOf(marker)
        if (idx < 0) return fallback
        return try {
            URLDecoder.decode(location.substring(idx + marker.length), "UTF-8")
        } catch (e: Exception) {
            fallback
        }
    }

    private fun resolveUrl(base: String, location: String): String =
        if (location.startsWith("http")) location else URL(URL(base), location).toString()

    companion object {
        const val LOGIN_POST_URL = "https://account.nicovideo.jp/api/v1/login?site=niconico&next_url=%2F"
    }
}
