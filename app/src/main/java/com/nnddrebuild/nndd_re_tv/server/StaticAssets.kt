package com.nnddrebuild.nndd_re_tv.server

import android.content.Context
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.FileNotFoundException

/**
 * app/src/main/assets/web/ 配下のHTML/CSS/JSをブラウザ向けに配信する。
 * Ktor標準の staticResources はJVMクラスパース前提でAndroidのassetsからは読めないため、
 * AssetManagerから直接読み出す薄いハンドラを用意する。
 */
fun Route.staticAssetRoutes(context: Context) {
    get("/") {
        respondAsset(call, context, "home.html")
    }
    get("/{path...}") {
        val path = call.parameters.getAll("path")?.joinToString("/").orEmpty()
        respondAsset(call, context, path.ifBlank { "home.html" })
    }
}

private suspend fun respondAsset(
    call: ApplicationCall,
    context: Context,
    assetPath: String
) {
    val bytes = try {
        context.assets.open("web/$assetPath").use { it.readBytes() }
    } catch (e: FileNotFoundException) {
        context.assets.open("web/home.html").use { it.readBytes() }
    }
    call.respondBytes(bytes, contentTypeFor(assetPath))
}

private fun contentTypeFor(path: String): ContentType = when {
    path.endsWith(".html") -> ContentType.Text.Html
    path.endsWith(".css") -> ContentType.Text.CSS
    path.endsWith(".js") -> ContentType.Text.JavaScript
    path.endsWith(".json") -> ContentType.Application.Json
    else -> ContentType.Text.Html
}
