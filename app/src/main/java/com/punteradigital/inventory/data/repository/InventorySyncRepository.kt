package com.punteradigital.inventory.data.repository

import android.util.Log
import com.punteradigital.inventory.data.local.SyncPreferences
import com.punteradigital.inventory.data.remote.CatalogModelSyncDto
import com.punteradigital.inventory.data.remote.InventoryRealtimeClient
import com.punteradigital.inventory.data.remote.InventorySyncEventDto
import com.punteradigital.inventory.data.remote.InventorySyncService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventorySyncRepository @Inject constructor(
    private val service: InventorySyncService,
    private val syncPreferences: SyncPreferences
) {
    private var realtimeClient: InventoryRealtimeClient? = null

    suspend fun fetchSnapshot(): List<Map<String, String>> {
        return service.getInventorySnapshot().body().orEmpty()
    }

    suspend fun fetchCatalogModels(): List<CatalogModelSyncDto> {
        if (!syncPreferences.isEnabled) return emptyList()
        return try {
            val response = service.getCatalogModels()
            Log.i("InventorySyncRepository", "GET ${syncPreferences.baseUrl}api/catalog/models -> ${response.code()} ${response.message()}")
            response.body().orEmpty()
        } catch (e: Exception) {
            Log.e("InventorySyncRepository", "Error fetching catalog models from central backend", e)
            emptyList()
        }
    }

    suspend fun sendInventoryEvent(event: InventorySyncEventDto): Boolean {
        if (!syncPreferences.isEnabled) {
            Log.w("InventorySyncRepository", "Central sync disabled; event skipped: ${event.entityType}/${event.action}")
            return false
        }

        return try {
            val response = service.sendInventoryChange(event)
            Log.i(
                "InventorySyncRepository",
                "POST ${syncPreferences.baseUrl}api/inventory/sync -> ${response.code()} ${response.message()}"
            )
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("InventorySyncRepository", "Error sending inventory event ${event.entityType}/${event.action}", e)
            false
        }
    }

    suspend fun sendInventoryBatch(events: List<InventorySyncEventDto>): Boolean {
        if (!syncPreferences.isEnabled) return false
        return try {
            val response = service.sendInventoryBatch(events)
            Log.i(
                "InventorySyncRepository",
                "POST ${syncPreferences.baseUrl}api/inventory/sync/batch -> ${response.code()} ${response.message()}"
            )
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("InventorySyncRepository", "Error sending batch inventory events", e)
            false
        }
    }

    fun connectRealtime(onEvent: (InventorySyncEventDto) -> Unit) {
        if (!syncPreferences.isEnabled) return
        realtimeClient?.disconnect()
        realtimeClient = InventoryRealtimeClient(
            wsUrl = syncPreferences.wsUrl,
            onEvent = onEvent,
            onConnectionStateChanged = {}
        )
        realtimeClient?.connect()
    }

    fun disconnectRealtime() {
        realtimeClient?.disconnect()
        realtimeClient = null
    }
}
