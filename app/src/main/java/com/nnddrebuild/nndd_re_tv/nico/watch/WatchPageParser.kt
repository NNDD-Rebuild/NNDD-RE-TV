package com.nnddrebuild.nndd_re_tv.nico.watch

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * watch v3 APIレスポンスのdataフィールドをWatchPageInfoへ変換する。
 * 移植元: NNDD-RE(PC版) src/main/nicovideo/watch/WatchPageParser.ts
 */
object WatchPageParser {

    fun parseApiData(dataJson: JsonElement): WatchPageInfo {
        val obj = dataJson.jsonObject
        val root = (obj["data"] ?: obj["response"] ?: dataJson).jsonObject

        val videoNode = root["video"]?.jsonObject ?: JsonObject(emptyMap())
        val mediaNode = root["media"]?.jsonObject ?: JsonObject(emptyMap())
        val commentNode = root["comment"]?.jsonObject ?: JsonObject(emptyMap())
        val clientNode = root["client"]?.jsonObject ?: JsonObject(emptyMap())
        val thumbnailNode = videoNode["thumbnail"]?.jsonObject ?: JsonObject(emptyMap())

        val domand = parseDomand(mediaNode)

        return WatchPageInfo(
            videoId = videoNode["id"]?.jsonPrimitive?.contentOrNull ?: "",
            title = videoNode["title"]?.jsonPrimitive?.contentOrNull ?: "",
            duration = videoNode["duration"]?.jsonPrimitive?.intOrNull ?: 0,
            thumbnailUrl = thumbnailNode["url"]?.jsonPrimitive?.contentOrNull ?: "",
            isDownloadable = domand.accessRightKey != null,
            commentThreads = parseCommentThreads(commentNode),
            threadKey = findThreadKey(commentNode),
            commentServerUrl = findCommentServerUrl(commentNode, clientNode),
            domandAccessRightKey = domand.accessRightKey,
            domandVideos = domand.videos,
            domandAudios = domand.audios,
            nvCommentParams = findNvCommentParams(commentNode),
            watchTrackId = clientNode["watchTrackId"]?.jsonPrimitive?.contentOrNull
        )
    }

    private data class DomandInfo(
        val accessRightKey: String?,
        val videos: List<DomandStreamCandidate>,
        val audios: List<DomandStreamCandidate>
    )

    private fun parseDomand(media: JsonObject): DomandInfo {
        val domand = media["domand"]?.jsonObject ?: return DomandInfo(null, emptyList(), emptyList())
        val accessRightKey = domand["accessRightKey"]?.jsonPrimitive?.contentOrNull
        val videos = (domand["videos"] as? JsonArray ?: JsonArray(emptyList())).map { toStreamCandidate(it.jsonObject) }
        val audios = (domand["audios"] as? JsonArray ?: JsonArray(emptyList())).map { toStreamCandidate(it.jsonObject) }
        return DomandInfo(accessRightKey, videos, audios)
    }

    private val labelFromIdRegex = Regex("""(\d+p)$""")

    private fun toStreamCandidate(obj: JsonObject): DomandStreamCandidate {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: ""
        val height = obj["height"]?.jsonPrimitive?.intOrNull
        val label = height?.let { "${it}p" }
            ?: labelFromIdRegex.find(id)?.groupValues?.get(1)
            ?: id
        return DomandStreamCandidate(
            id = id,
            isAvailable = obj["isAvailable"]?.jsonPrimitive?.booleanOrNull ?: false,
            qualityLevel = obj["qualityLevel"]?.jsonPrimitive?.intOrNull ?: 0,
            label = label
        )
    }

    private fun parseCommentThreads(comment: JsonObject): List<CommentThreadInfo> {
        val threadsArr = comment["threads"] as? JsonArray ?: JsonArray(emptyList())
        return threadsArr.map {
            val obj = it.jsonObject
            CommentThreadInfo(
                id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                fork = obj["fork"]?.jsonPrimitive?.contentOrNull ?: "main",
                isActive = obj["isActive"]?.jsonPrimitive?.booleanOrNull ?: false
            )
        }
    }

    private fun findThreadKey(comment: JsonObject): String? {
        val nv = comment["nvComment"]?.jsonObject
        nv?.get("threadKey")?.jsonPrimitive?.contentOrNull?.let { return it }
        comment["threadKey"]?.jsonPrimitive?.contentOrNull?.let { return it }
        val threads = comment["threads"] as? JsonArray ?: JsonArray(emptyList())
        for (t in threads) {
            t.jsonObject["threadkey"]?.jsonPrimitive?.contentOrNull?.let { return it }
        }
        return null
    }

    private fun findNvCommentParams(comment: JsonObject): NvCommentParams? {
        val nv = comment["nvComment"]?.jsonObject ?: return null
        val params = nv["params"]?.jsonObject ?: return null
        val targets = (params["targets"] as? JsonArray ?: JsonArray(emptyList())).map {
            val obj = it.jsonObject
            CommentTarget(
                id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                fork = obj["fork"]?.jsonPrimitive?.contentOrNull ?: "main"
            )
        }
        val language = params["language"]?.jsonPrimitive?.contentOrNull ?: "ja-jp"
        return NvCommentParams(targets, language)
    }

    private fun findCommentServerUrl(comment: JsonObject, @Suppress("UNUSED_PARAMETER") client: JsonObject): String {
        val nv = comment["nvComment"]?.jsonObject
        nv?.get("server")?.jsonPrimitive?.contentOrNull?.let { return it }
        return "https://public.nvcomment.nicovideo.jp"
    }
}
