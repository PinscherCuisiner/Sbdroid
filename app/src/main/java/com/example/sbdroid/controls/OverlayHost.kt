package com.example.sbdroid.controls

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner

/** Lets a plain Activity (oSBM's SDLActivity) host Compose. */
private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val reg = LifecycleRegistry(this)
    private val ssc = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = reg
    override val savedStateRegistry: SavedStateRegistry get() = ssc.savedStateRegistry
    override val viewModelStore = ViewModelStore()
    init { ssc.performRestore(null); reg.currentState = Lifecycle.State.RESUMED }
}

object OverlayHost {
    /** Called from oSBM's MainActivity.onCreate: draws SbDroid's controls over the game. */
    @JvmStatic
    fun attach(activity: Activity) {
        SdlInput.filesDir = activity.getExternalFilesDir(null)
        val owner = OverlayOwner()
        val items = LayoutStore(activity).load()
        val cv = ComposeView(activity)
        ViewTreeLifecycleOwner.set(cv, owner)
        ViewTreeSavedStateRegistryOwner.set(cv, owner)
        ViewTreeViewModelStoreOwner.set(cv, owner)
        cv.setContent {
            MaterialTheme { ControlsOverlay(items, false, {}, SdlInput::handle) }
        }
        activity.addContentView(cv, ViewGroup.LayoutParams(-1, -1))
        cv.addOnLayoutChangeListener { _, l, t, r, b, _, _, _, _ -> SdlInput.setScreen(r - l, b - t) }
    }
}
