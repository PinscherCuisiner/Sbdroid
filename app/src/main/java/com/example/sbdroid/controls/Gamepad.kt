package com.example.sbdroid.controls

import android.view.KeyEvent

/** Bluetooth / USB gamepad buttons -> the same control ids the touch overlay uses. */
object Gamepad {
    private val map = mapOf(
        KeyEvent.KEYCODE_BUTTON_A to "jump",
        KeyEvent.KEYCODE_BUTTON_B to "alt",
        KeyEvent.KEYCODE_BUTTON_X to "interact",
        KeyEvent.KEYCODE_BUTTON_Y to "inventory",
        KeyEvent.KEYCODE_BUTTON_R1 to "fire",
        KeyEvent.KEYCODE_BUTTON_R2 to "fire",
        KeyEvent.KEYCODE_BUTTON_L1 to "alt",
        KeyEvent.KEYCODE_BUTTON_L2 to "alt",
        KeyEvent.KEYCODE_BUTTON_START to "chat",
        KeyEvent.KEYCODE_BUTTON_SELECT to "map",
        KeyEvent.KEYCODE_BUTTON_THUMBL to "walk",
        KeyEvent.KEYCODE_BUTTON_THUMBR to "swap",
        KeyEvent.KEYCODE_DPAD_LEFT to "hotbar_prev",
        KeyEvent.KEYCODE_DPAD_RIGHT to "hotbar_next",
        KeyEvent.KEYCODE_DPAD_UP to "emote",
        KeyEvent.KEYCODE_DPAD_DOWN to "drop",
    )
    fun idFor(keyCode: Int): String? = map[keyCode]
    const val DEADZONE = 0.15f
}
