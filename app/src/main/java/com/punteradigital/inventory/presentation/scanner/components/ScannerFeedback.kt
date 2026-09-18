package com.punteradigital.inventory.presentation.scanner.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

fun vibrateSuccess(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrate(context, VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE), null)
    } else {
        vibrate(context, null, 150)
    }
}

fun vibrateError(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrate(context, VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1), null)
    } else {
        runCatching {
            (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
                ?.vibrate(longArrayOf(0, 300, 150, 300), -1)
        }.onFailure { Log.e("Scanner", "Vibrate error", it) }
    }
}

private fun vibrate(context: Context, effect: VibrationEffect?, legacyDuration: Long?) {
    runCatching {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(effect)
        } else if (legacyDuration != null) {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(legacyDuration)
        }
    }.onFailure { Log.e("Scanner", "Vibrate error", it) }
}
