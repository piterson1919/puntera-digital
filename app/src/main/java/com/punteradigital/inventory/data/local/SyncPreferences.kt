package com.punteradigital.inventory.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "central_sync_prefs"
        private const val KEY_BASE_URL = "central_base_url"
        private const val KEY_WS_URL = "central_ws_url"
        private const val KEY_ENABLED = "central_sync_enabled"

        private const val DEFAULT_BASE_URL = "http://192.168.0.121:8081/"
        private const val DEFAULT_WS_URL = "ws://192.168.0.121:8081/ws/inventory"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit().putString(KEY_BASE_URL, normalizeBaseUrl(value)).apply()

    var wsUrl: String
        get() = prefs.getString(KEY_WS_URL, DEFAULT_WS_URL) ?: DEFAULT_WS_URL
        set(value) = prefs.edit().putString(KEY_WS_URL, normalizeWsUrl(value)).apply()

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    fun save(baseUrl: String, wsUrl: String, enabled: Boolean = true) {
        this.baseUrl = baseUrl
        this.wsUrl = wsUrl
        this.isEnabled = enabled
    }

    private fun normalizeBaseUrl(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private fun normalizeWsUrl(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.startsWith("ws://") || trimmed.startsWith("wss://")) trimmed else {
            if (trimmed.startsWith("http://")) trimmed.replace("http://", "ws://")
            else if (trimmed.startsWith("https://")) trimmed.replace("https://", "wss://")
            else "ws://$trimmed"
        }
    }
}
