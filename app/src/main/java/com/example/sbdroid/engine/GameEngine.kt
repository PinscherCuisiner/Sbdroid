package com.example.sbdroid.engine

import android.util.Log
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface InputEvent {
    /** Joystick: x/y in -1..1 (y down is positive). */
    data class Axis(val id: String, val x: Float, val y: Float) : InputEvent
    /** Button or emote: id like "jump", "hotbar_3", "emote_happy". */
    data class Button(val id: String, val pressed: Boolean) : InputEvent
}

/**
 * gamePak  : the user's own packed.pak
 * modPaks  : enabled mods, already staged in load order (001_x.pak, 002_y.pak ...)
 * sbinit   : generated sbinit.config (assetDirectories / storageDirectory / logDirectory)
 */
data class EngineConfig(val gamePak: File, val modPaks: List<File>, val sbinit: File? = null)

/** Plug the real engine (an OpenStarbound NDK port such as oSBM) in by implementing this. */
interface GameEngine {
    val status: StateFlow<String>
    fun start(config: EngineConfig)
    fun send(event: InputEvent)
    fun stop()
}

class StubEngine : GameEngine {
    private val _status = MutableStateFlow("Engine not attached")
    override val status: StateFlow<String> = _status.asStateFlow()

    override fun start(config: EngineConfig) {
        _status.value = "Stub engine: ${config.gamePak.name} + ${config.modPaks.size} mod(s) staged"
        Log.d("SbDroid", _status.value)
    }

    override fun send(event: InputEvent) {
        Log.v("SbDroid", event.toString())
    }

    override fun stop() { _status.value = "Engine not attached" }
}
