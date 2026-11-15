package com.example.sbdroid.controls

import android.view.KeyEvent
import com.example.sbdroid.engine.InputEvent
import org.libsdl.app.SDLActivity
import kotlin.math.hypot

/** Turns SbDroid control events into the keyboard/mouse input oSBM (SDL) already understands. */
object SdlInput {
    private const val LEFT = 1
    private const val RIGHT = 2

    // Starbound default binds. Uncertain ones: quests, beam, drop, swap, zoom (rebind in-game if needed).
    private val keys = mapOf(
        "jump" to KeyEvent.KEYCODE_SPACE, "interact" to KeyEvent.KEYCODE_E,
        "inventory" to KeyEvent.KEYCODE_I, "chat" to KeyEvent.KEYCODE_ENTER,
        "map" to KeyEvent.KEYCODE_M, "crafting" to KeyEvent.KEYCODE_C,
        "codex" to KeyEvent.KEYCODE_X, "quests" to KeyEvent.KEYCODE_Q,
        "beam" to KeyEvent.KEYCODE_B, "drop" to KeyEvent.KEYCODE_T,
        "swap" to KeyEvent.KEYCODE_Z, "walk" to KeyEvent.KEYCODE_SHIFT_LEFT,
        "zoom" to KeyEvent.KEYCODE_EQUALS, "screenshot" to KeyEvent.KEYCODE_F12,
        "hotbar_1" to KeyEvent.KEYCODE_1, "hotbar_2" to KeyEvent.KEYCODE_2,
        "hotbar_3" to KeyEvent.KEYCODE_3, "hotbar_4" to KeyEvent.KEYCODE_4,
        "hotbar_5" to KeyEvent.KEYCODE_5, "hotbar_6" to KeyEvent.KEYCODE_6,
        "hotbar_7" to KeyEvent.KEYCODE_7, "hotbar_8" to KeyEvent.KEYCODE_8,
        "hotbar_9" to KeyEvent.KEYCODE_9, "hotbar_10" to KeyEvent.KEYCODE_0,
    )

    private val held = HashSet<Int>()
    private var mouseState = 0
    private var w = 1920f
    private var h = 1080f
    private var cx = 0f
    private var cy = 0f
    var filesDir: java.io.File? = null

    private val emotes = mapOf(
        "happy" to "Happy", "sad" to "Sad", "annoyed" to "Annoyed", "laugh" to "Laugh",
        "wink" to "Wink", "oh" to "Oh", "sleep" to "Sleep", "blabber" to "Blabbering",
    )

    fun setScreen(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            val first = cx == 0f && cy == 0f
            w = width.toFloat(); h = height.toFloat()
            if (first) { cx = w * 0.75f; cy = h * 0.5f }
        }
    }

    fun handle(e: InputEvent) {
        runCatching {
            when (e) {
                is InputEvent.Axis -> if (e.id == "move") move(e.x, e.y) else aim(e.x, e.y)
                is InputEvent.Button -> button(e.id, e.pressed)
            }
        }
    }

    private fun key(code: Int, down: Boolean) {
        if (down) { if (held.add(code)) SDLActivity.onNativeKeyDown(code) }
        else if (held.remove(code)) SDLActivity.onNativeKeyUp(code)
    }

    private fun move(x: Float, y: Float) {
        key(KeyEvent.KEYCODE_A, x < -0.35f)
        key(KeyEvent.KEYCODE_D, x > 0.35f)
        key(KeyEvent.KEYCODE_W, y < -0.5f)
        key(KeyEvent.KEYCODE_S, y > 0.5f)
    }

    private fun aim(x: Float, y: Float) {
        if (hypot(x, y) < 0.1f) return          // released: cursor stays where it was
        cx = (w / 2f + x * w * 0.35f).coerceIn(0f, w - 1)
        cy = (h / 2f + y * h * 0.42f).coerceIn(0f, h - 1)
        moveCursor()
    }

    private fun moveCursor() =
        SDLActivity.onNativeMouse(mouseState, if (mouseState != 0) 2 else 7, cx, cy, false)

    private fun mouse(button: Int, down: Boolean) {
        moveCursor()
        mouseState = if (down) mouseState or button else mouseState and button.inv()
        SDLActivity.onNativeMouse(mouseState, if (down) 0 else 1, cx, cy, false)
    }

    private fun button(id: String, down: Boolean) {
        when (id) {
            "fire" -> mouse(LEFT, down)
            "alt" -> mouse(RIGHT, down)
            else -> if (id.startsWith("emote_")) {
                if (down) emotes[id.removePrefix("emote_")]?.let { filesDir?.let { d -> java.io.File(d, "sbdroid_emote.txt").writeText(it) } }
            } else keys[id]?.let { key(it, down) }
        }
    }
}
