package com.punteradigital.inventory.data.remote

import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * BarTender Integration Builder endpoint.
 * Sends JSON payload via POST to http://[IP]:8080/Integration/PunteraDigital_QR/Execute
 */
interface PrintService {

    @Headers("Content-Type: application/json")
    @POST("Integration/PunteraDigital_QR/Execute")
    suspend fun printLabels(@Body jsonBody: RequestBody): Response<Unit>
}

/**
 * Helper to build the JSON payload for BarTender.
 */
object PrintCsvBuilder {
    fun buildJsonPayload(item: PrintLabelItem): String {
        return JSONObject().apply {
            put("UUID", item.uuid)
            put("Modelo", item.model)
            put("Talla", item.size)
            put("Lote", item.lot)
            put("Origen", item.origin)
        }.toString()
    }
}

data class PrintLabelItem(
    val uuid: String,
    val model: String,
    val size: String,
    val lot: String,
    val origin: String
)
