package com.example.remotecontrol

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI

/**
 * عميل بسيط للتواصل مع signaling server عن طريق WebSocket.
 * مسؤول عن:
 *  - تسجيل الجهاز وأخذ كود الاقتران
 *  - استقبال إشعار لما جهاز تحكم يتصل
 *  - تمرير رسائل WebRTC (offer/answer/ice) من وإلى WebRtcManager
 */
class SignalingClient(
    private val serverUrl: String,
    private val onCodeReceived: (String) -> Unit,
    private val onControllerJoined: () -> Unit
) {
    private var onSignalReceived: ((JSONObject) -> Unit)? = null
    private lateinit var client: WebSocketClient

    fun setOnSignalReceived(callback: (JSONObject) -> Unit) {
        onSignalReceived = callback
    }

    fun connect() {
        client = object : WebSocketClient(URI(serverUrl)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                send(JSONObject().apply { put("type", "register-device") }.toString())
            }

            override fun onMessage(message: String?) {
                if (message == null) return
                val json = JSONObject(message)
                when (json.getString("type")) {
                    "registered" -> onCodeReceived(json.getString("code"))
                    "controller-joined" -> onControllerJoined()
                    "signal" -> onSignalReceived?.invoke(json.getJSONObject("data"))
                    "controller-disconnected" -> { /* ممكن توقف البث هنا لو حابب */ }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {}
            override fun onError(ex: Exception?) {}
        }
        client.connect()
    }

    fun sendSignal(data: JSONObject) {
        val msg = JSONObject().apply {
            put("type", "signal")
            put("data", data)
        }
        client.send(msg.toString())
    }

    fun disconnect() {
        if (::client.isInitialized) client.close()
    }
}
