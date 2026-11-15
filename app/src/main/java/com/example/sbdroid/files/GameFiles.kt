package com.example.sbdroid.files

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

sealed interface GameState {
    data object Missing : GameState
    data class Importing(val progress: Float) : GameState
    data class Ready(val sizeMb: Long) : GameState
    data class Error(val msg: String) : GameState
}

/** Copies the user's own packed.pak into app-private storage. */
class GameFiles(private val ctx: Context) {
    private val file = File(ctx.filesDir, "game/packed.pak")
    private val _state = MutableStateFlow<GameState>(
        if (file.exists()) GameState.Ready(file.length() / 1_048_576) else GameState.Missing
    )
    val state: StateFlow<GameState> = _state.asStateFlow()
    val pak: File? get() = file.takeIf { it.exists() }

    suspend fun import(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            if (!PakValidator.isPak(ctx, uri)) {
                _state.value = GameState.Error("Not a Starbound .pak (missing SBAsset6 header)")
                return@withContext
            }
            file.parentFile!!.mkdirs()
            val tmp = File(file.path + ".tmp")
            val total = ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            ctx.contentResolver.openInputStream(uri)!!.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(1 shl 16)
                    var copied = 0L
                    var lastPct = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        copied += n
                        val pct = if (total > 0) (copied * 100 / total).toInt() else 0
                        if (total > 0 && pct != lastPct) {
                            lastPct = pct
                            _state.value = GameState.Importing(copied.toFloat() / total)
                        }
                    }
                }
            }
            file.delete()
            tmp.renameTo(file)
            _state.value = GameState.Ready(file.length() / 1_048_576)
        } catch (e: Exception) {
            _state.value = GameState.Error(e.message ?: "Import failed")
        }
    }
}
