package com.example.sbdroid.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** List order == load order (later mods override earlier ones). */
data class ModEntry(val fileName: String, val displayName: String, val enabled: Boolean)

class ModManager(private val ctx: Context) {
    private val dir = File(ctx.filesDir, "mods").apply { mkdirs() }
    private val prefs = ctx.getSharedPreferences("mods", Context.MODE_PRIVATE)
    private val _mods = MutableStateFlow(load())
    val mods: StateFlow<List<ModEntry>> = _mods.asStateFlow()

    private fun load(): List<ModEntry> {
        val arr = JSONArray(prefs.getString("list", "[]"))
        return (0 until arr.length()).map { arr.getJSONObject(it) }
            .map { ModEntry(it.getString("f"), it.getString("n"), it.getBoolean("e")) }
            .filter { File(dir, it.fileName).exists() }
    }

    private fun commit(list: List<ModEntry>) {
        _mods.value = list
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("f", it.fileName).put("n", it.displayName).put("e", it.enabled)) }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    suspend fun import(uri: Uri): Result<ModEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val name = ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && i >= 0) c.getString(i) else null
            } ?: "mod.pak"
            require(PakValidator.isPak(ctx, uri)) { "$name is not a valid Starbound .pak" }
            val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            var target = File(dir, safe)
            var n = 1
            while (target.exists()) target = File(dir, "${n++}_$safe")
            ctx.contentResolver.openInputStream(uri)!!.use { i -> target.outputStream().use { i.copyTo(it) } }
            val entry = ModEntry(target.name, name.removeSuffix(".pak"), true)
            commit(_mods.value + entry)
            entry
        }
    }

    fun toggle(fileName: String) =
        commit(_mods.value.map { if (it.fileName == fileName) it.copy(enabled = !it.enabled) else it })

    fun move(fileName: String, delta: Int) {
        val list = _mods.value.toMutableList()
        val i = list.indexOfFirst { it.fileName == fileName }
        val j = i + delta
        if (i < 0 || j !in list.indices) return
        list.add(j, list.removeAt(i))
        commit(list)
    }

    fun remove(fileName: String) {
        File(dir, fileName).delete()
        commit(_mods.value.filterNot { it.fileName == fileName })
    }

    fun enabledFiles(): List<File> = _mods.value.filter { it.enabled }.map { File(dir, it.fileName) }

    /**
     * Puts enabled mods into [target] as 001_name.pak, 002_name.pak ... so that the numeric prefix
     * keeps the user's load order even if the engine just scans a folder alphabetically.
     * Uses hard links when possible (no extra storage), otherwise copies.
     */
    fun stage(target: File): List<File> {
        target.mkdirs()
        val wanted = enabledFiles().mapIndexed { i, f -> f to File(target, "%03d_%s".format(i + 1, f.name)) }
        val keep = wanted.map { it.second.name }.toSet()
        target.listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
        wanted.forEach { (src, dst) ->
            if (!dst.exists() || dst.length() != src.length()) {
                dst.delete()
                runCatching { java.nio.file.Files.createLink(dst.toPath(), src.toPath()) }
                    .onFailure { src.copyTo(dst, overwrite = true) }
            }
        }
        return wanted.map { it.second }
    }
}
