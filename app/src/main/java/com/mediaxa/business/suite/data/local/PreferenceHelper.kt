package com.mediaxa.business.suite.data.local

import android.content.Context
import android.content.SharedPreferences

object PreferenceHelper {
    private const val PREFS_NAME = "mediaxa_sync_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_STORE_UUID = "store_uuid"
    private const val KEY_USER_UUID = "user_uuid"
    private const val KEY_LAST_SYNC_TIME = "last_sync_time"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveTokens(context: Context, accessToken: String, refreshToken: String, storeUuid: String, userUuid: String) {
        getPrefs(context).edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_STORE_UUID, storeUuid)
            putString(KEY_USER_UUID, userUuid)
            apply()
        }
    }

    fun getAccessToken(context: Context): String? = getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(context: Context): String? = getPrefs(context).getString(KEY_REFRESH_TOKEN, null)
    fun getStoreUuid(context: Context): String? = getPrefs(context).getString(KEY_STORE_UUID, null)
    fun getUserUuid(context: Context): String? = getPrefs(context).getString(KEY_USER_UUID, null)

    fun getLastSyncTime(context: Context): Long = getPrefs(context).getLong(KEY_LAST_SYNC_TIME, 0L)
    fun saveLastSyncTime(context: Context, timestamp: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply()
    }
    
    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
