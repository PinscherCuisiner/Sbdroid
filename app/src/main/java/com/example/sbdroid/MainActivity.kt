package com.example.sbdroid

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.sbdroid.controls.Gamepad
import com.example.sbdroid.engine.InputEvent
import com.example.sbdroid.ui.AppRoot

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { AppRoot(vm) }
    }

    // ---- Gamepad support (buttons) ----
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val id = Gamepad.idFor(keyCode)
        if (id != null && event.repeatCount == 0) { vm.send(InputEvent.Button(id, true)); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val id = Gamepad.idFor(keyCode)
        if (id != null) { vm.send(InputEvent.Button(id, false)); return true }
        return super.onKeyUp(keyCode, event)
    }

    // ---- Gamepad support (sticks): left = move, right = aim ----
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val isStick = (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        if (isStick && event.action == MotionEvent.ACTION_MOVE) {
            fun dz(v: Float) = if (kotlin.math.abs(v) < Gamepad.DEADZONE) 0f else v
            vm.send(InputEvent.Axis("move", dz(event.getAxisValue(MotionEvent.AXIS_X)), dz(event.getAxisValue(MotionEvent.AXIS_Y))))
            vm.send(InputEvent.Axis("aim", dz(event.getAxisValue(MotionEvent.AXIS_Z)), dz(event.getAxisValue(MotionEvent.AXIS_RZ))))
            return true
        }
        return super.onGenericMotionEvent(event)
    }
}
