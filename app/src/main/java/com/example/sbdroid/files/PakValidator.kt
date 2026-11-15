package com.example.sbdroid.files

import android.content.Context
import android.net.Uri

object PakValidator {
    private val MAGIC = "SBAsset6".toByteArray()

    /** Starbound .pak files (base game and mods) start with "SBAsset6". */
    fun isPak(ctx: Context, uri: Uri): Boolean = try {
        ctx.contentResolver.openInputStream(uri)?.use { s ->
            val buf = ByteArray(MAGIC.size)
            s.read(buf) == MAGIC.size && buf.contentEquals(MAGIC)
        } ?: false
    } catch (e: Exception) { false }
}
