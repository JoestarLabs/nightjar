package com.bl4ckswordsman.nightjar.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.bl4ckswordsman.nightjar.data.SunsetAnimation
import com.bl4ckswordsman.nightjar.ui.components.FluidLiquidOverlay
import com.bl4ckswordsman.nightjar.ui.components.MaterialShapesOverlay
import com.bl4ckswordsman.nightjar.ui.components.RisingWaveOverlay
import com.bl4ckswordsman.nightjar.ui.components.SnowflakesOverlay
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme

/**
 * Minimal [LifecycleOwner], [ViewModelStoreOwner], and [SavedStateRegistryOwner] enabling
 * Jetpack Compose composition within an Android Foreground Service window.
 */
class ServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)

    init {
        controller.performRestore(null)
    }

    /** Transitions the lifecycle state to [Lifecycle.State.STARTED]. */
    fun start() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    /** Transitions the lifecycle state to [Lifecycle.State.DESTROYED] and clears the view model store. */
    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
}

/**
 * Manages the floating system overlay window displaying sunset warning animations.
 *
 * @param context Application or service context used to access [WindowManager].
 */
class ComposeOverlayManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var lifecycleOwner: ServiceLifecycleOwner? = null

    // State holders for Compose content updates
    private val remainingSecondsState = mutableLongStateOf(0L)
    private val totalSecondsState = mutableLongStateOf(0L)
    private val tiltState = mutableFloatStateOf(0f)
    private val gravityAngleRadState = mutableFloatStateOf(0f)
    private val animationState = mutableStateOf(SunsetAnimation.FLUID_LIQUID)
    private val startedAtMillisState = mutableLongStateOf(0L)

    /** Indicates whether the overlay window is currently active and attached. */
    val isShowing: Boolean get() = composeView != null

    /**
     * Attaches and displays the sunset animation overlay with the specified parameters.
     *
     * @param remainingSeconds Seconds remaining in the countdown.
     * @param totalSeconds Total duration of the sunset warning window.
     * @param tilt Normalized lateral device tilt in `[-1.0, 1.0]`.
     * @param gravityAngleRad Full 2D gravity orientation angle in radians.
     * @param animation Visual sunset animation style to render.
     * @param startedAtMillis Epoch-ms timestamp when the timer started.
     */
    fun show(
        remainingSeconds: Long,
        totalSeconds: Long,
        tilt: Float = 0f,
        gravityAngleRad: Float = 0f,
        animation: SunsetAnimation = SunsetAnimation.FLUID_LIQUID,
        startedAtMillis: Long? = null
    ) {
        remainingSecondsState.longValue = remainingSeconds
        totalSecondsState.longValue = totalSeconds
        tiltState.floatValue = tilt
        gravityAngleRadState.floatValue = gravityAngleRad
        animationState.value = animation
        startedAtMillisState.longValue = startedAtMillis ?: 0L

        if (composeView != null) return

        val lifecycleOwner = ServiceLifecycleOwner().also { this.lifecycleOwner = it }
        lifecycleOwner.start()

        val view = createComposeView(lifecycleOwner)
        val params = createLayoutParams()

        windowManager.addView(view, params)
        this.composeView = view
    }

    /** Creates and configures the [ComposeView] hosting the sunset animation composables. */
    private fun createComposeView(lifecycleOwner: ServiceLifecycleOwner): ComposeView {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            fitsSystemWindows = false

            // Set Tree Owners for Compose to work outside Activity
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)

            setContent {
                NightjarTheme {
                    when (animationState.value) {
                        SunsetAnimation.OFF -> Unit
                        SunsetAnimation.FLUID_LIQUID -> {
                            FluidLiquidOverlay(
                                remainingSeconds = remainingSecondsState.longValue,
                                totalSeconds = totalSecondsState.longValue,
                                gravityAngleRad = gravityAngleRadState.floatValue,
                                startedAtMillis = startedAtMillisState.longValue.takeIf { it > 0L }
                            )
                        }
                        SunsetAnimation.CLASSIC_WAVE -> {
                            RisingWaveOverlay(
                                remainingSeconds = remainingSecondsState.longValue,
                                totalSeconds = totalSecondsState.longValue,
                                tilt = tiltState.floatValue,
                                startedAtMillis = startedAtMillisState.longValue.takeIf { it > 0L }
                            )
                        }
                        SunsetAnimation.MATERIAL_SHAPES -> {
                            MaterialShapesOverlay(
                                remainingSeconds = remainingSecondsState.longValue,
                                totalSeconds = totalSecondsState.longValue,
                                gravityAngleRad = gravityAngleRadState.floatValue,
                                startedAtMillis = startedAtMillisState.longValue.takeIf { it > 0L }
                            )
                        }
                        SunsetAnimation.SNOWFLAKES -> {
                            SnowflakesOverlay(
                                remainingSeconds = remainingSecondsState.longValue,
                                totalSeconds = totalSecondsState.longValue,
                                gravityAngleRad = gravityAngleRadState.floatValue,
                                startedAtMillis = startedAtMillisState.longValue.takeIf { it > 0L }
                            )
                        }
                    }
                }
            }
        }
    }

    /** Creates [WindowManager.LayoutParams] configured for an untargetable, transparent system overlay. */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
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
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    /** Updates the remaining countdown time displayed by the overlay. */
    fun updateRemainingTime(remainingSeconds: Long) {
        remainingSecondsState.longValue = remainingSeconds
    }

    /** Updates the lateral tilt factor used by the classic wave animation. */
    fun updateTilt(tilt: Float) {
        tiltState.floatValue = tilt
    }

    /** Updates sensor values for physical gravity alignment across 360 degrees. */
    fun updateSensors(tilt: Float, gravityAngleRad: Float) {
        tiltState.floatValue = tilt
        gravityAngleRadState.floatValue = gravityAngleRad
    }

    /** Dismisses and removes the overlay window from the window manager. */
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
