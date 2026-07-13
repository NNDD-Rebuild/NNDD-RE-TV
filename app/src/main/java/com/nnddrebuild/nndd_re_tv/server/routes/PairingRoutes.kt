package com.nnddrebuild.nndd_re_tv.server.routes

import com.nnddrebuild.nndd_re_tv.server.PairingManager
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.pairingRoutes(pairingManager: PairingManager) {
    get("/pair") {
        val token = call.request.queryParameters["token"]
        val sessionId = token?.let { pairingManager.tryPair(it) }
        if (sessionId == null) {
            call.respondText(
                "ペアリングに失敗しました。TV側で新しいQRコードを表示してから再度お試しください。",
                status = HttpStatusCode.Forbidden
            )
            return@get
        }
        call.response.cookies.append(
            Cookie(
                name = PairingManager.SESSION_COOKIE_NAME,
                value = sessionId,
                httpOnly = true,
                path = "/",
                maxAge = 60 * 60 * 24 * 30
            )
        )
        call.respondRedirect("/")
    }
}
