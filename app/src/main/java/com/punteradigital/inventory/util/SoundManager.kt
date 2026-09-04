package com.punteradigital.inventory.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio feedback for scan operations.
 * - Beep sound on successful scan (burst mode)
 * - Error tone on validation failure
 *
 * ToneGenerator is created lazily and auto-released after [IDLE_TIMEOUT_MS]
 * of inactivity to avoid holding audio hardware resources permanently.
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Release audio hardware after 30 seconds of inactivity */
        private const val IDLE_TIMEOUT_MS = 30_000L
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Lazily allocated — only holds audio hardware during active scanner sessions
    private var _toneGenerator: ToneGenerator? = null
    private var idleJob: Job? = null

    private fun acquireTone(): ToneGenerator {
        idleJob?.cancel()
        if (_toneGenerator == null) {
            _toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        }
        // Schedule auto-release after idle timeout
        idleJob = scope.launch {
            delay(IDLE_TIMEOUT_MS)
            release()
        }
        return _toneGenerator!!
    }

    /** Short beep for successful rapid scan */
    fun playSuccessBeep() {
        try { acquireTone().startTone(ToneGenerator.TONE_PROP_ACK, 100) }
        catch (e: Exception) { /* Audio may be unavailable on some devices */ }
    }

    /** Error beep for validation failure */
    fun playErrorBeep() {
        try { acquireTone().startTone(ToneGenerator.TONE_PROP_NACK, 300) }
        catch (e: Exception) { /* Audio may be unavailable on some devices */ }
    }

    /** Critical alert sound for Poka-Yoke violations */
    fun playCriticalAlert() {
        try { acquireTone().startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 500) }
        catch (e: Exception) { /* Audio may be unavailable on some devices */ }
    }

    /** Release audio hardware — called automatically after idle timeout */
    fun release() {
        idleJob?.cancel()
        idleJob = null
        _toneGenerator?.release()
        _toneGenerator = null
    }
}
