package com.example.sbdroid.controls

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class ControlType { JOYSTICK, BUTTON, EMOTE }

/** x/y are fractions (0..1) of the screen, so layouts adapt to any screen size. */
data class ControlItem(
    val id: String,
    val type: ControlType,
    val label: String,
    val x: Float,
    val y: Float,
    val sizeDp: Float,
    val opacity: Float = 0.6f,
    val visible: Boolean = true,
)

object DefaultLayout {
    fun create(): List<ControlItem> = buildList {
        add(ControlItem("move", ControlType.JOYSTICK, "Move", 0.12f, 0.70f, 150f))
        add(ControlItem("aim", ControlType.JOYSTICK, "Aim", 0.86f, 0.52f, 130f))
        add(ControlItem("jump", ControlType.BUTTON, "Jump", 0.74f, 0.84f, 64f))
        add(ControlItem("interact", ControlType.BUTTON, "Use", 0.66f, 0.68f, 52f))
        add(ControlItem("fire", ControlType.BUTTON, "Atk", 0.94f, 0.84f, 64f))
        add(ControlItem("alt", ControlType.BUTTON, "Alt", 0.84f, 0.87f, 52f))
        add(ControlItem("inventory", ControlType.BUTTON, "Inv", 0.95f, 0.08f, 46f))
        add(ControlItem("emote", ControlType.EMOTE, "Emote", 0.88f, 0.08f, 46f))
        add(ControlItem("chat", ControlType.BUTTON, "Chat", 0.81f, 0.08f, 46f))
        add(ControlItem("map", ControlType.BUTTON, "Map", 0.74f, 0.08f, 46f))
        for (i in 1..6) add(ControlItem("hotbar_$i", ControlType.BUTTON, "$i", 0.28f + 0.07f * (i - 1), 0.92f, 44f))
        // Extra top row (menus and utility actions)
        listOf("crafting" to "Craft", "codex" to "Codex", "quests" to "Quest", "beam" to "Beam",
            "drop" to "Drop", "swap" to "Swap", "walk" to "Walk")
            .forEachIndexed { i, (id, label) -> add(ControlItem(id, ControlType.BUTTON, label, 0.16f + 0.07f * i, 0.08f, 40f)) }
        // Hidden by default; enable them in "Customize controls"
        for (i in 7..10) add(ControlItem("hotbar_$i", ControlType.BUTTON, if (i == 10) "0" else "$i",
            0.30f + 0.07f * (i - 7), 0.80f, 40f, visible = false))
        add(ControlItem("zoom", ControlType.BUTTON, "Zoom", 0.60f, 0.08f, 40f, visible = false))
        add(ControlItem("screenshot", ControlType.BUTTON, "Shot", 0.67f, 0.08f, 40f, visible = false))
    }
}

/** Left-handed layout: flips every control horizontally. */
fun List<ControlItem>.mirrored(): List<ControlItem> = map { it.copy(x = 1f - it.x) }

val EMOTES = listOf("happy", "sad", "annoyed", "laugh", "wink", "oh", "sleep", "blabber")

class LayoutStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("controls", Context.MODE_PRIVATE)

    fun load(): List<ControlItem> {
        val saved = prefs.getString("layout", null) ?: return DefaultLayout.create()
        return runCatching {
            val arr = JSONArray(saved)
            val loaded = (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                ControlItem(
                    o.getString("id"), ControlType.valueOf(o.getString("t")), o.getString("l"),
                    o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.getDouble("s").toFloat(),
                    o.getDouble("o").toFloat(), o.getBoolean("v"),
                )
            }
            // Controls added in newer versions show up automatically in old saved layouts
            val have = loaded.map { it.id }.toSet()
            loaded + DefaultLayout.create().filter { it.id !in have }
        }.getOrElse { DefaultLayout.create() }
    }

    fun save(items: List<ControlItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("id", it.id).put("t", it.type.name).put("l", it.label)
                .put("x", it.x.toDouble()).put("y", it.y.toDouble()).put("s", it.sizeDp.toDouble())
                .put("o", it.opacity.toDouble()).put("v", it.visible))
        }
        prefs.edit().putString("layout", arr.toString()).apply()
    }
}
