package com.ailauncher.app.data

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.ailauncher.app.domain.models.AppCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v10: situational hints for the ranking, read on demand from system services
 * that need no permission. Nothing is stored or sent anywhere.
 */
data class DeviceContext(
    val headphonesConnected: Boolean = false,
) {
    /** 0..1 boost for a category in this situation. */
    fun boostFor(category: AppCategory): Float =
        when {
            headphonesConnected && category == AppCategory.ENTERTAINMENT -> 1f
            headphonesConnected && category == AppCategory.SOCIAL -> 0.3f
            else -> 0f
        }
}

@Singleton
class ContextSignals
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val audioManager by lazy { context.getSystemService(AudioManager::class.java) }

        fun current(): DeviceContext = DeviceContext(headphonesConnected = headphonesConnected())

        private fun headphonesConnected(): Boolean =
            try {
                audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.any { it.type in HEADPHONE_TYPES } == true
            } catch (_: Exception) {
                false
            }

        private companion object {
            val HEADPHONE_TYPES =
                setOf(
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    26, // TYPE_BLE_HEADSET (API 31; literal so it compiles against minSdk 26 checks)
                )
        }
    }
