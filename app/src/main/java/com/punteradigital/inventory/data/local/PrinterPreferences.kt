package com.punteradigital.inventory.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages printer configuration stored in SharedPreferences.
 * Stores BarTender server IP, port, and related settings.
 */
@Singleton
class PrinterPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "printer_config"
        private const val KEY_IP = "server_ip"
        private const val KEY_PORT = "server_port"
        private const val KEY_PROTOCOL = "protocol"
        private const val KEY_RETRY_ENABLED = "retry_enabled"
        private const val KEY_OFFLINE_QUEUE_ENABLED = "offline_queue_enabled"
        private const val KEY_TIMEOUT_SECONDS = "timeout_seconds"
        private const val KEY_IS_CONFIGURED = "is_configured"

        // Defaults
        private const val DEFAULT_IP = "192.168.0.50"
        private const val DEFAULT_PORT = 8080
        private const val DEFAULT_PROTOCOL = "http"
        private const val DEFAULT_TIMEOUT = 10
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverIp: String
        get() = prefs.getString(KEY_IP, DEFAULT_IP) ?: DEFAULT_IP
        set(value) = prefs.edit().putString(KEY_IP, value).apply()

    var serverPort: Int
        get() = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        set(value) = prefs.edit().putInt(KEY_PORT, value).apply()

    var protocol: String
        get() = prefs.getString(KEY_PROTOCOL, DEFAULT_PROTOCOL) ?: DEFAULT_PROTOCOL
        set(value) = prefs.edit().putString(KEY_PROTOCOL, value).apply()

    var retryEnabled: Boolean
        get() = prefs.getBoolean(KEY_RETRY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_RETRY_ENABLED, value).apply()

    var offlineQueueEnabled: Boolean
        get() = prefs.getBoolean(KEY_OFFLINE_QUEUE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_OFFLINE_QUEUE_ENABLED, value).apply()

    var timeoutSeconds: Int
        get() = prefs.getInt(KEY_TIMEOUT_SECONDS, DEFAULT_TIMEOUT)
        set(value) = prefs.edit().putInt(KEY_TIMEOUT_SECONDS, value).apply()

    var isConfigured: Boolean
        get() = prefs.getBoolean(KEY_IS_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_CONFIGURED, value).apply()

    /**
     * Constructs the full base URL for Retrofit.
     * Example: http://192.168.0.50:8080/
     */
    fun getBaseUrl(): String = "$protocol://$serverIp:$serverPort/"

    /**
     * Saves all configuration at once.
     */
    fun saveConfig(
        ip: String,
        port: Int,
        protocol: String = "http",
        retryEnabled: Boolean = true,
        offlineQueueEnabled: Boolean = true,
        timeoutSeconds: Int = 10
    ) {
        prefs.edit()
            .putString(KEY_IP, ip)
            .putInt(KEY_PORT, port)
            .putString(KEY_PROTOCOL, protocol)
            .putBoolean(KEY_RETRY_ENABLED, retryEnabled)
            .putBoolean(KEY_OFFLINE_QUEUE_ENABLED, offlineQueueEnabled)
            .putInt(KEY_TIMEOUT_SECONDS, timeoutSeconds)
            .putBoolean(KEY_IS_CONFIGURED, true)
            .apply()
    }
}
