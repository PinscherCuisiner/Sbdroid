package com.example.sbdroid

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sbdroid.controls.ControlItem
import com.example.sbdroid.controls.DefaultLayout
import com.example.sbdroid.controls.LayoutStore
import com.example.sbdroid.controls.mirrored
import com.example.sbdroid.engine.EngineConfig
import com.example.sbdroid.engine.GameEngine
import com.example.sbdroid.engine.InputEvent
import com.example.sbdroid.engine.StubEngine
import com.example.sbdroid.files.GameFiles
import com.example.sbdroid.files.ModManager
import com.example.sbdroid.files.SbInit
import java.io.File
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val gameFiles = GameFiles(app)
    val mods = ModManager(app)
    private val store = LayoutStore(app)
    private val engine: GameEngine = com.example.sbdroid.engine.OsbmEngine(app)

    val layout = MutableStateFlow(store.load())
    val message = MutableStateFlow<String?>(null)

    fun updateItem(item: ControlItem) = layout.update { l -> l.map { if (it.id == item.id) item else it } }
    fun saveLayout() = store.save(layout.value)
    fun resetLayout() { layout.value = DefaultLayout.create(); saveLayout() }
    fun mirrorLayout() { layout.update { it.mirrored() }; saveLayout() }

    fun importGame(uri: Uri) = viewModelScope.launch { gameFiles.import(uri) }
    fun importMods(uris: List<Uri>) = viewModelScope.launch {
        uris.forEach { u -> mods.import(u).onFailure { message.value = it.message } }
    }

    fun startEngine() {
        val pak = gameFiles.pak ?: return
        val root = File(getApplication<Application>().filesDir, "engine")
        val modsDir = File(root, "mods")
        val staged = mods.stage(modsDir)
        val sbinit = File(root, "sbinit.config")
        SbInit.write(sbinit, listOf(pak.parentFile!!, modsDir), File(root, "storage"), File(root, "logs"))
        engine.start(EngineConfig(pak, staged, sbinit))
    }
    fun stopEngine() = engine.stop()
    val engineStatus: StateFlow<String> get() = engine.status
    fun send(e: InputEvent) = engine.send(e)
}
