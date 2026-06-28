package com.mediaxa.business.suite.data.sync

import android.content.Context
import android.util.Log

private const val TAG = "DeviceIdManager"
private const val PREFS_NAME = "mediaxa_device_prefs"
private const val KEY_DEVICE_ID = "device_id"

/**
 * Manages a persistent, unique device identifier for multi-device sync support.
 *
 * The device ID is generated once as a UUID v4 on first app launch and persisted
 * in SharedPreferences. It is stable across app updates and database migrations.
 *
 * Security note: Using regular SharedPreferences (not Encrypted) intentionally —
 * the device ID is not sensitive data; it is only used for sync conflict resolution.
 * EncryptedSharedPreferences requires API 23+ and adds complexity without security benefit here.
 *
 * In Phase 10, this will be superseded by a server-assigned device registration flow.
 */
object DeviceIdManager {

    @Volatile
    private var cachedDeviceId: String? = null

    /**
     * Returns the persistent device ID, creating it on first call.
     * Thread-safe via double-checked locking.
     */
    fun getOrCreate(context: Context): String {
        return cachedDeviceId ?: synchronized(this) {
            cachedDeviceId ?: createOrLoadDeviceId(context).also { cachedDeviceId = it }
        }
    }

    private fun createOrLoadDeviceId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) {
            Log.d(TAG, "Loaded existing deviceId: $existing")
            return existing
        }
        val newId = "DEV-${java.util.UUID.randomUUID()}"
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        Log.d(TAG, "Created new deviceId: $newId")
        return newId
    }

    /** For testing purposes only — resets the cached device ID without touching SharedPreferences. */
    internal fun resetCache() {
        cachedDeviceId = null
    }
}
