package com.punteradigital.inventory.data.repository

import android.util.Log
import com.punteradigital.inventory.data.local.PrinterPreferences
import com.punteradigital.inventory.data.remote.PrintCsvBuilder
import com.punteradigital.inventory.data.remote.PrintLabelItem
import com.punteradigital.inventory.data.remote.PrintService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for printing labels via BarTender.
 * Handles retry logic with exponential backoff, error categorization,
 * and offline queue fallback.
 */
@Singleton
class PrintRepository @Inject constructor(
    private val printerPreferences: PrinterPreferences,
    private val syncManager: SyncManager
) {
    private val TAG = "PrintRepository"

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 500L
        private const val BACKOFF_MULTIPLIER = 2.0
        private const val TEST_TIMEOUT_MS = 5000 // 5s for connection test — faster feedback
    }

    // Cached client — avoids rebuilding TCP connection pools on every print call.
    // Invalidated when printer IP/port configuration changes.
    private var cachedBaseUrl: String? = null
    private var cachedPrintService: PrintService? = null
    private var cachedOkHttpClient: OkHttpClient? = null

    /**
     * Result of a print operation.
     */
    sealed class PrintResult {
        data class Success(val labelCount: Int) : PrintResult()
        data class Queued(val message: String) : PrintResult()
        data class Error(val error: PrintError) : PrintResult()
    }

    /**
     * Categorized errors for user-friendly feedback.
     */
    sealed class PrintError(val userMessage: String, val technicalMessage: String) {
        class NotConfigured : PrintError(
            "Impresora no configurada",
            "PrinterPreferences.isConfigured = false"
        )
        class NetworkUnreachable(ip: String) : PrintError(
            "No se puede conectar al servidor ($ip)",
            "Network unreachable or host unknown"
        )
        class ConnectionTimeout(ip: String) : PrintError(
            "Tiempo de espera agotado conectando a $ip",
            "Socket timeout exceeded"
        )
        class ServerError(code: Int) : PrintError(
            "BarTender respondió con error (código $code)",
            "HTTP $code response from BarTender"
        )
        class NotBarTender(ip: String) : PrintError(
            "El servidor $ip respondió, pero NO es BarTender. Verifique la IP y el puerto.",
            "HTTP response received but does not match BarTender Integration Builder signature"
        )
        class InvalidData(detail: String) : PrintError(
            "Datos de etiqueta inválidos: $detail",
            "CSV build validation failed: $detail"
        )
        class Unknown(cause: Throwable) : PrintError(
            "Error inesperado al imprimir",
            cause.message ?: "Unknown error"
        )
    }

    /**
     * Print labels with automatic retry and offline queue fallback.
     *
     * Flow:
     * 1. Validate config & data
     * 2. Attempt direct print (with retries if enabled)
     * 3. On failure, queue for later sync if offline queue is enabled
     * 4. Return user-friendly result
     */
    suspend fun printLabels(items: List<PrintLabelItem>): PrintResult {
        // Validation
        if (!printerPreferences.isConfigured) {
            return PrintResult.Error(PrintError.NotConfigured())
        }
        if (items.isEmpty()) {
            return PrintResult.Error(PrintError.InvalidData("Lista de etiquetas vacía"))
        }

        val printService = buildPrintService()
        val maxAttempts = if (printerPreferences.retryEnabled) MAX_RETRIES else 1
        var successCount = 0
        var lastException: Exception? = null

        // Iterate and send one POST request per label mapping to BarTender's JSON variables
        for (item in items) {
            val json = try { PrintCsvBuilder.buildJsonPayload(item) } catch (e: Exception) { continue }
            val body = json.toRequestBody("application/json".toMediaType())
            var itemSuccess = false

            for (attempt in 1..maxAttempts) {
                try {
                    val response = printService.printLabels(body)
                    if (response.isSuccessful) {
                        itemSuccess = true
                        successCount++
                        break
                    } else {
                        if (response.code() in 400..499) {
                            lastException = RuntimeException("HTTP ${response.code()}")
                            break
                        }
                        lastException = RuntimeException("HTTP ${response.code()}")
                    }
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < maxAttempts) {
                        val delayMs = (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, (attempt - 1).toDouble())).toLong()
                        delay(delayMs)
                    }
                }
            }

            // Stop if an item completely fails (network down), we queue the rest
            if (!itemSuccess) {
                Log.e(TAG, "Failed to print item ${item.uuid}, stopping batch")
                break
            }
        }

        if (successCount == items.size) {
            Log.i(TAG, "✅ Print batch successful: $successCount labels")
            return PrintResult.Success(successCount)
        } else {
            val failedItems = items.drop(successCount)
            if (printerPreferences.offlineQueueEnabled && failedItems.isNotEmpty()) {
                try {
                    syncManager.enqueuePrintJob(failedItems)
                    return PrintResult.Queued("Impresas $successCount. ${failedItems.size} guardadas en cola por error.")
                } catch (e: Exception) {
                    return PrintResult.Error(categorizePrintError(lastException ?: e))
                }
            }
            return PrintResult.Error(categorizePrintError(lastException ?: RuntimeException("Batch failure")))
        }
    }

    /**
     * Test connection to the BarTender server using a 2-phase approach:
     *
     * Phase 1: Raw TCP socket connect — verifies the host:port is reachable
     *          at the network level (no HTTP). This catches wrong IPs fast.
     *
     * Phase 2: HTTP POST to the Integration endpoint — verifies BarTender
     *          Integration Builder is actually running and listening.
     *          Validates the response is NOT from a proxy/router/captive portal
     *          by checking response characteristics.
     */
    suspend fun testConnection(): PrintResult {
        if (!printerPreferences.isConfigured) {
            return PrintResult.Error(PrintError.NotConfigured())
        }

        val ip = printerPreferences.serverIp
        val port = printerPreferences.serverPort

        // — Phase 1: TCP Socket Probe —
        // This catches unreachable IPs without waiting for HTTP overhead.
        try {
            Log.d(TAG, "Phase 1: TCP socket probe to $ip:$port")
            val socketReachable = withContext(Dispatchers.IO) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, port), TEST_TIMEOUT_MS)
                        true
                    }
                } catch (e: Exception) {
                    false
                }
            }
            if (!socketReachable) {
                Log.w(TAG, "Phase 1 FAILED: TCP socket to $ip:$port not reachable")
                return PrintResult.Error(PrintError.NetworkUnreachable(ip))
            }
            Log.d(TAG, "Phase 1 OK: TCP socket to $ip:$port reachable")
        } catch (e: Exception) {
            return PrintResult.Error(categorizePrintError(e))
        }

        // — Phase 2: HTTP POST to BarTender endpoint —
        // Use raw OkHttp (not Retrofit) to read the actual response body/headers
        // and verify it's really BarTender, not a proxy/router/captive portal.
        return try {
            Log.d(TAG, "Phase 2: HTTP POST to BarTender endpoint")
            val testJson = """{"UUID":"TEST-PING"}"""
            val url = "${printerPreferences.getBaseUrl()}Integration/PunteraDigital_QR/Execute"

            val testClient = OkHttpClient.Builder()
                .connectTimeout(TEST_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(TEST_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .writeTimeout(TEST_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(testJson.toRequestBody("application/json".toMediaType()))
                .build()

            val response = withContext(Dispatchers.IO) {
                testClient.newCall(request).execute()
            }

            val responseCode = response.code
            val responseBody = response.body?.string() ?: ""
            val contentType = response.header("Content-Type") ?: ""
            val server = response.header("Server") ?: ""
            response.close()

            Log.d(TAG, "Phase 2 response: code=$responseCode, body=${responseBody.take(200)}, server=$server")

            when {
                responseCode in 200..299 -> {
                    // Validate it's actually BarTender, not a random web server
                    val looksLikeBarTender = isBarTenderResponse(responseBody, contentType, server)
                    if (looksLikeBarTender) {
                        PrintResult.Success(0)
                    } else {
                        // Something responded 200 but it's NOT BarTender
                        Log.w(TAG, "Response 200 but NOT BarTender. Body: ${responseBody.take(300)}")
                        PrintResult.Error(PrintError.NotBarTender(ip))
                    }
                }
                // 404 = BarTender is running but the integration name is wrong
                responseCode == 404 -> {
                    PrintResult.Error(PrintError.ServerError(404))
                }
                // 405 = endpoint exists but method not allowed (unlikely with POST)
                responseCode == 405 -> {
                    PrintResult.Success(0)
                }
                else -> {
                    PrintResult.Error(PrintError.ServerError(responseCode))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Phase 2 FAILED: ${e.message}")
            PrintResult.Error(categorizePrintError(e))
        }
    }

    /**
     * Heuristic to determine if an HTTP response came from BarTender Integration Builder
     * vs. a router, proxy, captive portal, or random web server.
     *
     * BarTender Integration Builder typically:
     * - Returns XML with "BarTender" or "Integration" in the body
     * - Returns short JSON/XML responses, not full HTML pages
     * - Has Server header containing "BarTender" or "Seagull"
     * - Does NOT return large HTML pages with <html>, <head>, etc.
     */
    private fun isBarTenderResponse(body: String, contentType: String, server: String): Boolean {
        val bodyLower = body.lowercase()
        val serverLower = server.lowercase()
        val contentLower = contentType.lowercase()

        // Positive signals: BarTender-specific content
        if (serverLower.contains("bartender") || serverLower.contains("seagull")) return true
        if (bodyLower.contains("bartender")) return true
        if (bodyLower.contains("integration")) return true

        // Negative signals: definitely NOT BarTender
        // Full HTML page = router admin panel, captive portal, etc.
        if (bodyLower.contains("<html") && bodyLower.contains("<head")) return false
        if (bodyLower.contains("<!doctype")) return false
        // Login page or portal
        if (bodyLower.contains("login") && bodyLower.contains("password")) return false
        if (bodyLower.contains("captive")) return false

        // If body is short (< 500 chars) and not HTML, likely a real API response
        if (body.length < 500 && !bodyLower.contains("<html")) return true

        // If content-type is XML or JSON (not text/html), likely an API
        if (contentLower.contains("xml") || contentLower.contains("json")) return true

        // Empty body with 200 — could be BarTender acknowledging
        if (body.isEmpty()) return true

        // Default: suspicious — too large or HTML-like
        return false
    }

    /**
     * Builds or returns a cached PrintService.
     * Only rebuilds OkHttpClient+Retrofit when IP/port/timeout changes.
     * This allows TCP connection reuse (keep-alive) across multiple print jobs.
     */
    private fun buildPrintService(): PrintService {
        val currentBaseUrl = printerPreferences.getBaseUrl()
        val currentTimeout = printerPreferences.timeoutSeconds

        // Return cached instance if config hasn't changed
        val existing = cachedPrintService
        if (existing != null && cachedBaseUrl == currentBaseUrl) {
            return existing
        }

        // Config changed or first call — build new client
        Log.d(TAG, "Building new PrintService for $currentBaseUrl")
        val client = OkHttpClient.Builder()
            .connectTimeout(currentTimeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(currentTimeout.toLong(), TimeUnit.SECONDS)
            .writeTimeout(currentTimeout.toLong(), TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(client)
            .build()
        val service = retrofit.create(PrintService::class.java)

        cachedOkHttpClient = client
        cachedPrintService = service
        cachedBaseUrl = currentBaseUrl
        return service
    }

    /**
     * Maps low-level exceptions to user-friendly PrintError categories.
     */
    private fun categorizePrintError(e: Exception): PrintError {
        val ip = printerPreferences.serverIp
        return when (e) {
            is UnknownHostException -> PrintError.NetworkUnreachable(ip)
            is ConnectException -> PrintError.NetworkUnreachable(ip)
            is SocketTimeoutException -> PrintError.ConnectionTimeout(ip)
            else -> PrintError.Unknown(e)
        }
    }
}
