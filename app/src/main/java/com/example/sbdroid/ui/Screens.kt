package com.example.sbdroid.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sbdroid.AppViewModel
import com.example.sbdroid.controls.ControlsOverlay
import com.example.sbdroid.files.GameState

private enum class Screen { HOME, MODS, PLAY, EDIT }

@Composable
fun AppRoot(vm: AppViewModel) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    BackHandler(screen != Screen.HOME) { screen = Screen.HOME }
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            when (screen) {
                Screen.HOME -> HomeScreen(vm, { screen = it })
                Screen.MODS -> ModsScreen(vm) { screen = Screen.HOME }
                Screen.PLAY -> PlayScreen(vm, startInEdit = false) { screen = Screen.HOME }
                Screen.EDIT -> PlayScreen(vm, startInEdit = true) { screen = Screen.HOME }
            }
        }
    }
}

@Composable
private fun HomeScreen(vm: AppViewModel, go: (Screen) -> Unit) {
    val state by vm.gameFiles.state.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importGame) }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("SbDroid", style = MaterialTheme.typography.headlineMedium)
        Text("Select the packed.pak from your own copy of Starbound.")
        when (val s = state) {
            GameState.Missing -> Text("No game files loaded.")
            is GameState.Importing -> LinearProgressIndicator(progress = { s.progress }, Modifier.fillMaxWidth())
            is GameState.Ready -> Text("packed.pak ready (${s.sizeMb} MB)")
            is GameState.Error -> Text(s.msg, color = MaterialTheme.colorScheme.error)
        }
        msg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button({ picker.launch(arrayOf("*/*")) }) { Text("Select packed.pak") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ go(Screen.MODS) }) { Text("Mods") }
            OutlinedButton({ go(Screen.EDIT) }) { Text("Customize controls") }
        }
        Button({ go(Screen.PLAY) }, enabled = state is GameState.Ready) { Text("Play") }
    }
}

@Composable
private fun ModsScreen(vm: AppViewModel, back: () -> Unit) {
    val mods by vm.mods.mods.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { vm.importMods(it) }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(back) { Text("Back") }
            Text("Mods (load order, top first)", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Button({ picker.launch(arrayOf("*/*")) }) { Text("Import .pak") }
        }
        msg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (mods.isEmpty()) Text("No mods yet. Import .pak files you have downloaded.", Modifier.padding(16.dp))
        LazyColumn {
            items(mods, key = { it.fileName }) { m ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Switch(m.enabled, { vm.mods.toggle(m.fileName) })
                    Text(m.displayName, Modifier.weight(1f).padding(horizontal = 12.dp))
                    IconButton({ vm.mods.move(m.fileName, -1) }) { Icon(Icons.Default.KeyboardArrowUp, "Up") }
                    IconButton({ vm.mods.move(m.fileName, 1) }) { Icon(Icons.Default.KeyboardArrowDown, "Down") }
                    IconButton({ vm.mods.remove(m.fileName) }) { Icon(Icons.Default.Delete, "Remove") }
                }
            }
        }
    }
}

@Composable
private fun PlayScreen(vm: AppViewModel, startInEdit: Boolean, exit: () -> Unit) {
    val items by vm.layout.collectAsStateWithLifecycle()
    val status by vm.engineStatus.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf(startInEdit) }
    DisposableEffect(Unit) {
        if (!startInEdit) vm.startEngine()
        onDispose { vm.stopEngine(); vm.saveLayout() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // The engine's render surface goes here (e.g. an AndroidView wrapping a GLSurfaceView).
        Text(status, Modifier.align(Alignment.Center), color = Color.Gray)
        ControlsOverlay(items, edit, vm::updateItem, vm::send)

        Row(Modifier.align(Alignment.TopStart).systemBarsPadding().padding(8.dp)) {
            if (edit) {
                Button({ edit = false; vm.saveLayout(); if (startInEdit) exit() }) { Text("Done") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(vm::resetLayout) { Text("Reset") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(vm::mirrorLayout) { Text("Mirror") }
            } else {
                IconButton({ edit = true }) { Icon(Icons.Default.Settings, "Edit controls", tint = Color.White) }
                TextButton(exit) { Text("Exit") }
            }
        }
    }
}
