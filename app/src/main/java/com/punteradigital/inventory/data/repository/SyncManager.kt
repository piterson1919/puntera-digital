package com.punteradigital.inventory.data.repository

import android.util.Log
import com.punteradigital.inventory.data.local.PrinterPreferences
import com.punteradigital.inventory.data.local.dao.InventoryDao
import com.punteradigital.inventory.data.local.entity.SyncQueueItem
import com.punteradigital.inventory.data.remote.GoogleSheetsService
import com.punteradigital.inventory.data.remote.PrintCsvBuilder
import com.punteradigital.inventory.data.remote.PrintLabelItem
import com.punteradigital.inventory.data.remote.PrintService
import com.punteradigital.inventory.data.remote.SyncRequestDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages offline-first sync queue.
 * Processes pending operations when connectivity is restored.
 */
@Singleton
class SyncManager @Inject constructor(
    private val dao: InventoryDao,
    private val printerPreferences: PrinterPreferences,
    private val sheetsService: GoogleSheetsService
) {
    private val gson = Gson()
    private val TAG = "SyncManager"

    // Cached print service — rebuilt only when IP changes
    private var cachedBaseUrl: String? = null
    private var cachedPrintService: PrintService? = null

    companion object {
        const val ENDPOINT_BARTENDER = "BARTENDER"
        const val ENDPOINT_SHEETS = "SHEETS"
        const val SHEETS_URL = "https://script.google.com/macros/s/AKfycbxnJ647eqtHKyUzJng7jCVGoNKmu7Twi7qzmEcLizggrJYP5fjdT-lGF4eh8ipV_G2vhg/exec"
    }

    /**
     * Enqueue a print job for later sync.
     */
    suspend fun enqueuePrintJob(items: List<PrintLabelItem>) {
        val payload = gson.toJson(items)
        dao.insertSyncItem(
            SyncQueueItem(
                payload = payload,
                endpoint = ENDPOINT_BARTENDER
            )
        )
    }

    /**
     * Enqueue a sheets sync job.
     */
    suspend fun enqueueSheetsSync(request: SyncRequestDto) {
        val payload = gson.toJson(request)
        dao.insertSyncItem(
            SyncQueueItem(
                payload = payload,
                endpoint = ENDPOINT_SHEETS
            )
        )
    }

    /**
     * Try to send a print job immediately.
     * Returns true if successful, false if it should be queued.
     */
    suspend fun sendPrintJobNow(items: List<PrintLabelItem>): Boolean {
        return try {
            val printService = buildPrintService()
            var allSuccess = true
            for (item in items) {
                val json = try { PrintCsvBuilder.buildJsonPayload(item) } catch (e: Exception) { continue }
                val body = json.toRequestBody("application/json".toMediaType())
                val response = printService.printLabels(body)
                if (!response.isSuccessful) {
                    allSuccess = false
                    break
                }
            }
            allSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Print failed, will queue", e)
            false
        }
    }

    /**
     * Builds a dynamic PrintService using current printer preferences.
     * Cached per base URL to allow TCP connection reuse.
     */
    private fun buildPrintService(): PrintService {
        val currentBaseUrl = printerPreferences.getBaseUrl()
        val existing = cachedPrintService
        if (existing != null && cachedBaseUrl == currentBaseUrl) return existing

        val client = OkHttpClient.Builder()
            .connectTimeout(printerPreferences.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(printerPreferences.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(printerPreferences.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(client)
            .build()
        val service = retrofit.create(PrintService::class.java)
        cachedPrintService = service
        cachedBaseUrl = currentBaseUrl
        return service
    }

    /**
     * Try to send sheets sync immediately.
     * Returns true if successful, false if it should be queued.
     */
    suspend fun sendSheetsSyncNow(request: SyncRequestDto): Boolean {
        return try {
            val response = sheetsService.syncMovements(SHEETS_URL, request)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Sheets sync failed, will queue", e)
            false
        }
    }

    /**
     * Process all pending sync items.
     * Called when connectivity is restored.
     */
    suspend fun processPendingQueue(): Int {
        val pending = dao.getPendingSyncItems()
        var successCount = 0

        for (item in pending) {
            val success = when (item.endpoint) {
                ENDPOINT_BARTENDER -> {
                    try {
                        val type = object : TypeToken<List<PrintLabelItem>>() {}.type
                        val items: List<PrintLabelItem> = gson.fromJson(item.payload, type)
                        sendPrintJobNow(items)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process print queue item", e)
                        false
                    }
                }
                ENDPOINT_SHEETS -> {
                    try {
                        val request = gson.fromJson(item.payload, SyncRequestDto::class.java)
                        sendSheetsSyncNow(request)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process sheets queue item", e)
                        false
                    }
                }
                else -> false
            }

            if (success) {
                dao.markSynced(item.id)
                successCount++
            } else {
                dao.markFailed(item.id)
            }
        }

        return successCount
    }
}
