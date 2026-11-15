package com.example.sbdroid.engine

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.file.Files

/** Stages the pak + mods where oSBM looks, drops a flag so oSBM skips its launcher, starts the SDL activity. */
class OsbmEngine(private val ctx: Context) : GameEngine {
    private val _status = MutableStateFlow("Ready")
    override val status: StateFlow<String> = _status.asStateFlow()

    override fun start(config: EngineConfig) {
        // Same root oSBM resolves first (getExternalFilesDir), else filesDir
        val root = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        val dst = File(root, "assets/packed.pak").also { it.parentFile?.mkdirs() }
        if (!dst.exists() || dst.length() != config.gamePak.length()) {
            dst.delete()
            try { Files.createSymbolicLink(dst.toPath(), config.gamePak.toPath()) }
            catch (e: Throwable) { config.gamePak.copyTo(dst, overwrite = true) }
        }
        val mods = File(root, "mods").also { it.mkdirs() }
        mods.listFiles()?.filter { it.name.matches(Regex("\\d{3}_.*\\.pak")) }?.forEach { it.delete() }
        config.modPaks.forEach { it.copyTo(File(mods, it.name), overwrite = true) }
        File(root, "sbdroid_direct_launch.flag").writeText("1")
        ctx.startActivity(
            Intent().setClassName(ctx, "io.github.openstarbound.mobile.MainActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        _status.value = "Launching"
    }

    override fun send(event: InputEvent) {}
    override fun stop() { _status.value = "Ready" }
}
