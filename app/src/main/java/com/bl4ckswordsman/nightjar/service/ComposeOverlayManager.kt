package com.bl4ckswordsman.nightjar.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bl4ckswordsman.nightjar.ui.components.RisingWaveOverlay
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme

class ServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)

    init {
        controller.performRestore(null)
    }

    fun start() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
}

class ComposeOverlayManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var lifecycleOwner: ServiceLifecycleOwner? = null

    // State holders for Compose content updates
    private val remainingSecondsState = mutableLongStateOf(0L)
    private val totalSecondsState = mutableLongStateOf(0L)
    private val tiltState = mutableFloatStateOf(0f)

    val isShowing: Boolean get() = composeView != null

    fun show(remainingSeconds: Long, totalSeconds: Long, tilt: Float = 0f) {
        remainingSecondsState.longValue = remainingSeconds
        totalSecondsState.longValue = totalSeconds
        tiltState.floatValue = tilt

        if (composeView != null) return

        val lifecycleOwner = ServiceLifecycleOwner().also { this.lifecycleOwner = it }
        lifecycleOwner.start()

        val view = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            fitsSystemWindows = false
            
            // Set Tree Owners for Compose to work outside Activity
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)

            setContent {
                NightjarTheme {
                    RisingWaveOverlay(
                        remainingSeconds = remainingSecondsState.longValue,
                        totalSeconds = totalSecondsState.longValue,
                        tilt = tiltState.floatValue
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Support drawing behind cuts and status bars for a true fullscreen wave
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        windowManager.addView(view, params)
        this.composeView = view
    }

    fun updateRemainingTime(remainingSeconds: Long) {
        remainingSecondsState.longValue = remainingSeconds
    }

    fun updateTilt(tilt: Float) {
        tiltState.floatValue = tilt
    }

    fun dismiss() {
        val view = composeView ?: return
        composeView = null

        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            // View might already be detached or service destroyed
        }

        lifecycleOwner?.stop()
        lifecycleOwner = null
    }
}
