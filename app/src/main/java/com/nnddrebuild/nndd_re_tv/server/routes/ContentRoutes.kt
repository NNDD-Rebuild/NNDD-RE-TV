package com.nnddrebuild.nndd_re_tv.server.routes

import com.nnddrebuild.nndd_re_tv.library.LibraryClient
import com.nnddrebuild.nndd_re_tv.nico.mylist.MyListClient
import com.nnddrebuild.nndd_re_tv.nico.ranking.RankingClient
import com.nnddrebuild.nndd_re_tv.nico.search.SearchClient
import com.nnddrebuild.nndd_re_tv.server.BrowserNavigateBus
import com.nnddrebuild.nndd_re_tv.server.SearchResultBus
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

@Serializable
data class NavigateTargetResponse(val target: String? = null)

fun Route.contentRoutes(
    searchClient: SearchClient,
    myListClient: MyListClient,
    rankingClient: RankingClient,
    libraryClient: LibraryClient
) {
    get("/api/ui/navigate-target") {
        call.respond(NavigateTargetResponse(BrowserNavigateBus.consume()))
    }

    get("/api/search") {
        val q = call.request.queryParameters["q"] ?: ""
        val items = if (q.isBlank()) emptyList() else searchClient.search(q)
        SearchResultBus.update(items)
        call.respond(items)
    }

    get("/api/ranking") {
        val genre = call.request.queryParameters["genre"] ?: "all"
        val term = call.request.queryParameters["term"] ?: "24h"
        val items = try {
            rankingClient.fetch(genre, term)
        } catch (e: Exception) {
            android.util.Log.e("NnddContentRoutes", "fetchRanking failed", e)
            emptyList()
        }
        call.respond(items)
    }

    get("/api/mylist") {
        val mylists = try {
            myListClient.fetchAccountMylists()
        } catch (e: Exception) {
            android.util.Log.e("NnddContentRoutes", "fetchAccountMylists failed", e)
            emptyList()
        }
        call.respond(mylists)
    }

    get("/api/mylist/{id}") {
        val id = call.parameters["id"] ?: ""
        val items = try {
            myListClient.fetchMylistItems(id)
        } catch (e: Exception) {
            android.util.Log.e("NnddContentRoutes", "fetchMylistItems($id) failed", e)
            emptyList()
        }
        call.respond(items)
    }

    get("/api/library") {
        val items = try {
            libraryClient.fetchLibrary()
        } catch (e: Exception) {
            android.util.Log.e("NnddContentRoutes", "fetchLibrary failed", e)
            emptyList()
        }
        call.respond(items)
    }
}
