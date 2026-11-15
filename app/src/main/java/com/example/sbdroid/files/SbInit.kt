package com.example.sbdroid.files

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Writes an OpenStarbound-style sbinit.config (same keys as the upstream template). */
object SbInit {
    fun write(target: File, assetDirs: List<File>, storage: File, logs: File) {
        target.parentFile?.mkdirs()
        storage.mkdirs(); logs.mkdirs()
        val json = JSONObject()
            .put("assetDirectories", JSONArray(assetDirs.map { it.absolutePath + "/" }))
            .put("storageDirectory", storage.absolutePath + "/")
            .put("logDirectory", logs.absolutePath + "/")
        target.writeText(json.toString(2))
    }
}
