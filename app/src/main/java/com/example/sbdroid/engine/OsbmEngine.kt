package com.example.sbdroid.engine

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Real engine: launches the oSBM SDL activity (native libmain.so). oSBM draws its own touch controls. */
class OsbmEngine(private val ctx: Context) : GameEngine {
    private val _status = MutableStateFlow("oSBM ready")
    override val status: StateFlow<String> = _status.asStateFlow()

    override fun start(config: EngineConfig) {
        val i = Intent().setClassName(ctx, "io.github.openstarbound.mobile.MainActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("sbinit", config.sbinit?.absolutePath)
            .putExtra("packedPak", config.gamePak.absolutePath)
        ctx.startActivity(i)
        _status.value = "oSBM running"
    }

    override fun send(event: InputEvent) { /* oSBM handles input itself */ }
    override fun stop() { _status.value = "oSBM ready" }
}
