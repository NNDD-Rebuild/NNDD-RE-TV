package com.nnddrebuild.nndd_re_tv.server.routes

import com.nnddrebuild.nndd_re_tv.playback.PlaybackCommand
import com.nnddrebuild.nndd_re_tv.playback.PlaybackCommandBus
import com.nnddrebuild.nndd_re_tv.playback.PlaybackStateBus
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class PlayRequest(val videoId: String)

@Serializable
data class SeekRequest(val positionMs: Long)

@Serializable
data class QualityRequest(val qualityId: String)

@Serializable
data class PlaybackAck(val status: String = "ok")

fun Route.playbackRoutes() {
    post("/api/playback/play") {
        val req = call.receive<PlayRequest>()
        PlaybackCommandBus.emit(PlaybackCommand.Play(req.videoId))
        call.respond(PlaybackAck())
    }

    post("/api/playback/play-library") {
        val req = call.receive<PlayRequest>()
        PlaybackCommandBus.emit(PlaybackCommand.PlayLibraryVideo(req.videoId))
        call.respond(PlaybackAck())
    }

    post("/api/playback/pause") {
        PlaybackCommandBus.emit(PlaybackCommand.Pause)
        call.respond(PlaybackAck())
    }

    post("/api/playback/resume") {
        PlaybackCommandBus.emit(PlaybackCommand.Resume)
        call.respond(PlaybackAck())
    }

    post("/api/playback/seek") {
        val req = call.receive<SeekRequest>()
        PlaybackCommandBus.emit(PlaybackCommand.SeekTo(req.positionMs))
        call.respond(PlaybackAck())
    }

    post("/api/playback/quality") {
        val req = call.receive<QualityRequest>()
        PlaybackCommandBus.emit(PlaybackCommand.ChangeQuality(req.qualityId))
        call.respond(PlaybackAck())
    }

    get("/api/playback/state") {
        call.respond(PlaybackStateBus.state.value)
    }
}
