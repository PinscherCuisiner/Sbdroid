package com.example.sbdroid.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.window.Popup
import com.example.sbdroid.engine.InputEvent
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ControlsOverlay(
    items: List<ControlItem>,
    editMode: Boolean,
    onItemChange: (ControlItem) -> Unit,
    onInput: (InputEvent) -> Unit,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        items.filter { it.visible || editMode }.forEach { item ->
            key(item.id) {
                val current by rememberUpdatedState(item)
                val sizePx = with(density) { item.sizeDp.dp.toPx() }
                var mod = Modifier
                    .offset { IntOffset((item.x * w - sizePx / 2).roundToInt(), (item.y * h - sizePx / 2).roundToInt()) }
                    .size(item.sizeDp.dp)
                    .alpha(if (item.visible) item.opacity else 0.2f)
                if (editMode) {
                    mod = mod
                        .pointerInput(item.id) {
                            detectDragGestures(onDragStart = { selectedId = item.id }) { change, drag ->
                                change.consume()
                                onItemChange(current.copy(
                                    x = (current.x + drag.x / w).coerceIn(0f, 1f),
                                    y = (current.y + drag.y / h).coerceIn(0f, 1f),
                                ))
                            }
                        }
                        .pointerInput(item.id) { detectTapGestures { selectedId = item.id } }
                }
                Box(mod) {
                    when {
                        editMode -> StaticVisual(item, selected = selectedId == item.id)
                        item.type == ControlType.JOYSTICK -> Joystick(item.id, onInput)
                        item.type == ControlType.BUTTON -> ActionButton(item.id, item.label, onInput)
                        item.type == ControlType.EMOTE -> EmoteButton(item.label, onInput)
                    }
                }
            }
        }

        if (editMode) {
            items.firstOrNull { it.id == selectedId }?.let { sel ->
                EditPanel(sel, onItemChange, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun EditPanel(item: ControlItem, onChange: (ControlItem) -> Unit, modifier: Modifier) {
    Card(modifier.width(320.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text("Visible", fontSize = 12.sp)
                Switch(item.visible, { onChange(item.copy(visible = it)) }, Modifier.padding(start = 6.dp))
            }
            Text("Size", fontSize = 12.sp)
            Slider(item.sizeDp, { onChange(item.copy(sizeDp = it)) }, valueRange = 30f..220f)
            Text("Opacity", fontSize = 12.sp)
            Slider(item.opacity, { onChange(item.copy(opacity = it)) }, valueRange = 0.1f..1f)
        }
    }
}

@Composable
private fun StaticVisual(item: ControlItem, selected: Boolean) {
    Box(
        Modifier.fillMaxSize().clip(CircleShape)
            .background(Color.White.copy(0.2f))
            .border(if (selected) 3.dp else 2.dp, if (selected) Color.Yellow else Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(item.label, color = Color.White, fontSize = 12.sp) }
}

@Composable
private fun Joystick(id: String, onInput: (InputEvent) -> Unit) {
    val cb by rememberUpdatedState(onInput)
    var knob by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier.fillMaxSize()
            .drawBehind {
                val r = size.minDimension / 2
                drawCircle(Color.White.copy(0.15f), r)
                drawCircle(Color.White, r, style = Stroke(3f))
                drawCircle(Color.White.copy(0.6f), r * 0.38f, center = center + knob)
            }
            .pointerInput(id) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val radius = size.width / 2f
                    val c = Offset(size.width / 2f, size.height / 2f)
                    fun update(pos: Offset) {
                        var d = pos - c
                        val len = d.getDistance()
                        if (len > radius) d *= radius / len
                        knob = d
                        cb(InputEvent.Axis(id, d.x / radius, d.y / radius))
                    }
                    update(down.position)
                    drag(down.id) { ch -> ch.consume(); update(ch.position) }
                    knob = Offset.Zero
                    cb(InputEvent.Axis(id, 0f, 0f))
                }
            }
    )
}

@Composable
private fun ActionButton(id: String, label: String, onInput: (InputEvent) -> Unit) {
    val cb by rememberUpdatedState(onInput)
    var pressed by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxSize().clip(CircleShape)
            .background(Color.White.copy(if (pressed) 0.55f else 0.2f))
            .border(2.dp, Color.White, CircleShape)
            .pointerInput(id) {
                awaitEachGesture {
                    awaitFirstDown()
                    pressed = true
                    cb(InputEvent.Button(id, true))
                    waitForUpOrCancellation()
                    pressed = false
                    cb(InputEvent.Button(id, false))
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 12.sp) }
}

@Composable
private fun EmoteButton(label: String, onInput: (InputEvent) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxSize().clip(CircleShape)
            .background(Color.White.copy(0.2f)).border(2.dp, Color.White, CircleShape)
            .clickable { open = true },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 12.sp) }

    if (open) {
        Popup(alignment = Alignment.Center, onDismissRequest = { open = false }) {
            Box(
                Modifier.size(280.dp).clip(CircleShape).background(Color.Black.copy(0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                EMOTES.forEachIndexed { i, name ->
                    val a = Math.toRadians(-90.0 + i * 360.0 / EMOTES.size)
                    Box(
                        Modifier.offset((cos(a) * 100).dp, (sin(a) * 100).dp).size(60.dp)
                            .clip(CircleShape).background(Color.White.copy(0.25f))
                            .clickable {
                                onInput(InputEvent.Button("emote_$name", true))
                                onInput(InputEvent.Button("emote_$name", false))
                                open = false
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(name, color = Color.White, fontSize = 11.sp) }
                }
            }
        }
    }
}
