package com.bl4ckswordsman.nightjar.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Minimal AccessibilityService whose sole purpose is to call
 * [performGlobalAction](GLOBAL_ACTION_LOCK_SCREEN) when the timer expires.
 *
 * Design decisions to satisfy Google Play + user transparency:
 *  - `accessibilityEventTypes="typeNone"` → subscribes to zero UI events.
 *  - `canRetrieveWindowContent="false"` → never reads the screen.
 *  - `canPerformGestures="false"` → cannot inject touch events.
 *
 * This approach is preferred over DevicePolicyManager.lockNow() because it
 * does NOT disable the user's biometric (fingerprint / face) unlocking.
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = WeakReference(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    // Unused — we subscribe to no events
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object {
        /** Weak reference — cleared automatically when the service is unbound. */
        @Volatile private var instance: WeakReference<LockAccessibilityService>? = null

        /** @return true if the service is currently connected and ready. */
        fun isEnabled(): Boolean = instance?.get() != null

        /**
         * Triggers the device lock screen.
         * Must only be called after confirming [isEnabled] == true.
         * Returns true if the action was dispatched.
         */
        fun requestLock(): Boolean {
            val service = instance?.get() ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
