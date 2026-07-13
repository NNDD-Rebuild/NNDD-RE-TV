package com.nnddrebuild.nndd_re_tv.server.routes

import com.nnddrebuild.nndd_re_tv.nico.auth.AuthManager
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val mailTel: String, val password: String)

@Serializable
data class MfaRequest(val mfaSubmitUrl: String, val code: String)

@Serializable
data class AuthResponse(
    val status: String,
    val mfaSubmitUrl: String? = null,
    val message: String? = null
)

@Serializable
data class AuthStatusResponse(val loggedIn: Boolean, val nickname: String? = null)

fun Route.authRoutes(authManager: AuthManager) {
    get("/api/auth/status") {
        val loggedIn = authManager.isLoggedIn()
        val nickname = if (loggedIn) authManager.fetchNickname() else null
        call.respond(AuthStatusResponse(loggedIn, nickname))
    }

    post("/api/auth/login") {
        val req = call.receive<LoginRequest>()
        val result = authManager.login(req.mailTel, req.password)
        val response = when {
            result.ok -> AuthResponse(status = "ok")
            result.mfaRequired -> AuthResponse(status = "mfa_required", mfaSubmitUrl = result.mfaSubmitUrl)
            else -> AuthResponse(status = "error", message = result.error)
        }
        call.respond(response)
    }

    post("/api/auth/mfa") {
        val req = call.receive<MfaRequest>()
        val result = authManager.completeMfa(req.mfaSubmitUrl, req.code)
        val response = if (result.ok) {
            AuthResponse(status = "ok")
        } else {
            AuthResponse(status = "error", message = result.error)
        }
        call.respond(response)
    }

    post("/api/auth/logout") {
        authManager.logout()
        call.respond(AuthResponse(status = "ok"))
    }
}
