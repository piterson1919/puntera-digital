package com.punteradigital.inventory.data.remote

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class InventoryRealtimeClient(
    private val wsUrl: String,
    private val onEvent: (InventorySyncEventDto) -> Unit,
    private val onConnectionStateChanged: (Boolean) -> Unit = {}
) {
    private val client = OkHttpClient()
    private var socket: WebSocket? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun connect() {
        val request = Request.Builder().url(wsUrl).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.i("InventoryRealtimeClient", "Connected to realtime sync server")
                onConnectionStateChanged(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val event = json.decodeFromString(InventorySyncEventDto.serializer(), text)
                    onEvent(event)
                } catch (e: Exception) {
                    Log.e("InventoryRealtimeClient", "Invalid realtime payload: $text", e)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                onConnectionStateChanged(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e("InventoryRealtimeClient", "Realtime sync failure", t)
                onConnectionStateChanged(false)
            }
        })
    }

    fun disconnect() {
        socket?.close(1000, "Closed by client")
        onConnectionStateChanged(false)
    }
}
