package com.nnddrebuild.nndd_re_tv.server

import android.content.Context
import com.nnddrebuild.nndd_re_tv.library.LibraryClient
import com.nnddrebuild.nndd_re_tv.library.LibraryConnectionStore
import com.nnddrebuild.nndd_re_tv.nico.auth.AuthManager
import com.nnddrebuild.nndd_re_tv.nico.mylist.MyListClient
import com.nnddrebuild.nndd_re_tv.nico.ranking.RankingClient
import com.nnddrebuild.nndd_re_tv.nico.search.SearchClient
import com.nnddrebuild.nndd_re_tv.server.routes.authRoutes
import com.nnddrebuild.nndd_re_tv.server.routes.contentRoutes
import com.nnddrebuild.nndd_re_tv.server.routes.libraryConnectionRoutes
import com.nnddrebuild.nndd_re_tv.server.routes.pairingRoutes
import com.nnddrebuild.nndd_re_tv.server.routes.playbackRoutes
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * TVアプリ内蔵HTTPサーバー。同一Wi-Fi内のブラウザからのペアリング・操作を受け付ける。
 */
class NnddTvServer(
    private val appContext: Context,
    private val pairingManager: PairingManager,
    private val authManager: AuthManager,
    private val searchClient: SearchClient,
    private val myListClient: MyListClient,
    private val rankingClient: RankingClient,
    private val libraryConnectionStore: LibraryConnectionStore,
    private val libraryClient: LibraryClient,
    private val port: Int = DEFAULT_PORT
) {

    private var server: EmbeddedServer<*, *>? = null

    fun start() {
        if (server != null) return
        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            module(
                appContext,
                pairingManager,
                authManager,
                searchClient,
                myListClient,
                rankingClient,
                libraryConnectionStore,
                libraryClient
            )
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 500, timeoutMillis = 1000)
        server = null
    }

    fun getPort(): Int = port

    companion object {
        const val DEFAULT_PORT = 23456
    }
}

private fun Application.module(
    appContext: Context,
    pairingManager: PairingManager,
    authManager: AuthManager,
    searchClient: SearchClient,
    myListClient: MyListClient,
    rankingClient: RankingClient,
    libraryConnectionStore: LibraryConnectionStore,
    libraryClient: LibraryClient
) {
    install(ContentNegotiation) {
        json()
    }
    routing {
        get("/health") {
            call.respondText(
                """{"ok":true,"app":"nndd-re-tv"}""",
                ContentType.Application.Json
            )
        }
        pairingRoutes(pairingManager)
        authRoutes(authManager)
        contentRoutes(searchClient, myListClient, rankingClient, libraryClient)
        playbackRoutes()
        libraryConnectionRoutes(libraryConnectionStore)
        staticAssetRoutes(appContext)
    }
}
