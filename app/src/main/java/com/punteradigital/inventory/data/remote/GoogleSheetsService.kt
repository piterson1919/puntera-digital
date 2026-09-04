package com.punteradigital.inventory.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Google Apps Script sync service.
 * Sends JSON inventory data to the deployed web app URL.
 */
data class SyncRequestDto(
    val title: String,
    val origin: String,
    val movements: List<InventoryMovementDto>
)

data class InventoryMovementDto(
    val date: String,
    val time: String,
    val userId: String,
    val type: String,
    val model: String,
    val size: String,
    val lot: String,
    val uuid: String,
    val origin: String,
    val status: String,
    val reason: String,
    val cliente: String = "",
    val observaciones: String = ""
)

interface GoogleSheetsService {
    @POST
    suspend fun syncMovements(
        @Url url: String,
        @Body request: SyncRequestDto
    ): Response<Unit>
}
