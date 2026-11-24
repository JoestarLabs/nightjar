package com.bl4ckswordsman.nightjar.receiver

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.getSystemService
import com.bl4ckswordsman.nightjar.R

class LockDeviceAdminReceiver : DeviceAdminReceiver() {
    companion object {
        private const val TAG = "LockDeviceAdminReceiver"

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, LockDeviceAdminReceiver::class.java)
        }

        fun isEnabled(context: Context): Boolean {
            val dpm = context.getSystemService<DevicePolicyManager>() ?: return false
            return dpm.isAdminActive(getComponentName(context))
        }

        fun requestLock(context: Context): Boolean {
            val dpm = context.getSystemService<DevicePolicyManager>() ?: return false
            if (!dpm.isAdminActive(getComponentName(context))) return false
            return try {
                dpm.lockNow()
                true
            } catch (e: SecurityException) {
                Log.w(TAG, "Device admin lockNow failed", e)
                false
            }
        }

        fun getRegisterIntent(context: Context): Intent {
            return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    context.getString(R.string.device_admin_explanation)
                )
            }
        }
    }
}
