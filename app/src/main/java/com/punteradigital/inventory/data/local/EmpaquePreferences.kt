package com.punteradigital.inventory.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmpaquePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "empaque_config"
        private const val KEY_ORIGIN = "last_origin"
        private const val KEY_MODEL = "last_model"
        private const val KEY_LOT = "last_lot"
        private const val KEY_FORMAT = "last_format"
        private const val KEY_IS_MASTER = "last_is_master"
        private const val KEY_CHILD_COUNT = "last_child_count"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var lastOrigin: String
        get() = prefs.getString(KEY_ORIGIN, "FOOT_SAFE") ?: "FOOT_SAFE"
        set(value) = prefs.edit().putString(KEY_ORIGIN, value).apply()

    var lastModel: String
        get() = prefs.getString(KEY_MODEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var lastLot: String
        get() = prefs.getString(KEY_LOT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LOT, value).apply()

    var lastFormat: String
        get() = prefs.getString(KEY_FORMAT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FORMAT, value).apply()

    var lastIsMaster: Boolean
        get() = prefs.getBoolean(KEY_IS_MASTER, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_MASTER, value).apply()

    var lastChildCount: Int
        get() = prefs.getInt(KEY_CHILD_COUNT, 8)
        set(value) = prefs.edit().putInt(KEY_CHILD_COUNT, value).apply()
}
