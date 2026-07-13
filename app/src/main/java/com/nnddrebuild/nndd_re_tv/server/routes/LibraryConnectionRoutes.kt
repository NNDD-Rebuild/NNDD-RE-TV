package com.nnddrebuild.nndd_re_tv.server.routes

import com.nnddrebuild.nndd_re_tv.library.LibraryConnection
import com.nnddrebuild.nndd_re_tv.library.LibraryConnectionStore
import com.nnddrebuild.nndd_re_tv.library.LibraryHttpClient
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

@Serializable
data class LibraryConnectionResponse(val host: String? = null, val port: Int? = null)

@Serializable
data class LibraryConnectionRequest(val host: String, val port: Int)

@Serializable
data class LibraryConnectionTestResponse(val ok: Boolean)

/** libraryClientへの反映はNnddTvApplication側のconnectionFlow購読で行われるため、ここではconnectionStoreへの保存のみ担当する。 */
fun Route.libraryConnectionRoutes(connectionStore: LibraryConnectionStore) {
    get("/api/library-connection") {
        val current = connectionStore.connectionFlow.first()
        call.respond(LibraryConnectionResponse(current?.host, current?.port))
    }

    post("/api/library-connection") {
        val req = call.receive<LibraryConnectionRequest>()
        connectionStore.save(req.host, req.port)
        call.respond(LibraryConnectionResponse(req.host, req.port))
    }

    post("/api/library-connection/test") {
        val req = call.receive<LibraryConnectionRequest>()
        val ok = LibraryHttpClient().checkHealth(LibraryConnection(req.host, req.port))
        call.respond(LibraryConnectionTestResponse(ok))
    }
}
