package com.punteradigital.inventory.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class InventorySyncEventDto(
    val eventId: String,
    val entityType: String,
    val action: String,
    val payload: Map<String, String>,
    val userId: String,
    val deviceId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class CatalogModelSyncDto(
    val id: String,
    val code: String,
    val name: String,
    val sizeMin: Int = 36,
    val sizeMax: Int = 46,
    val pairsPerBox: Int = 8,
    val isActive: Boolean = true,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

interface InventorySyncService {
    @GET("api/inventory")
    suspend fun getInventorySnapshot(): Response<List<Map<String, String>>>

    @GET("api/catalog/models")
    suspend fun getCatalogModels(): Response<List<CatalogModelSyncDto>>

    @POST("api/inventory/sync")
    suspend fun sendInventoryChange(@Body event: InventorySyncEventDto): Response<Unit>

    @POST("api/inventory/sync/batch")
    suspend fun sendInventoryBatch(@Body events: List<InventorySyncEventDto>): Response<Unit>
}
