package com.nnddrebuild.nndd_re_tv.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * QRペアリングのトークン発行・検証と、ペアリング済みブラウザセッションの管理。
 * 使い捨てのpairingTokenをQRに埋め込み、交換に成功したブラウザには長期のセッションIDを発行する。
 */
class PairingManager {

    private var pairingToken: String = UUID.randomUUID().toString()
    private var pairingTokenExpiresAt: Long = System.currentTimeMillis() + PAIRING_TOKEN_TTL_MS

    private val sessionIds = mutableSetOf<String>()

    private val _pairedCount = MutableStateFlow(0)
    val pairedCount: StateFlow<Int> = _pairedCount

    @Synchronized
    fun currentPairingToken(): String {
        if (System.currentTimeMillis() > pairingTokenExpiresAt) {
            regenerateToken()
        }
        return pairingToken
    }

    @Synchronized
    fun regenerateToken() {
        pairingToken = UUID.randomUUID().toString()
        pairingTokenExpiresAt = System.currentTimeMillis() + PAIRING_TOKEN_TTL_MS
    }

    /** トークンが正当なら新しいセッションIDを発行する。失敗時はnull。 */
    @Synchronized
    fun tryPair(token: String): String? {
        if (token != pairingToken || System.currentTimeMillis() > pairingTokenExpiresAt) {
            return null
        }
        val sessionId = UUID.randomUUID().toString()
        sessionIds += sessionId
        _pairedCount.value = sessionIds.size
        return sessionId
    }

    @Synchronized
    fun isValidSession(sessionId: String?): Boolean =
        sessionId != null && sessionId in sessionIds

    companion object {
        const val SESSION_COOKIE_NAME = "nndd_tv_session"
        private const val PAIRING_TOKEN_TTL_MS = 10 * 60 * 1000L
    }
}
