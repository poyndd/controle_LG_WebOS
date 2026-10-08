package com.andre.lgremote

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.TimeUnit

class LgRemoteClient(private val context: Context) {

    private val TAG = "LgRemoteClient"
    private val prefsName = "lgwebos_prefs"
    private val keyName = "client_key"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null
    private var connected = false

    private val subnets = listOf(
        "192.168.0",
        "192.168.1",
        "192.168.68",
        "10.0.0",
        "172.16.0"
    )

    fun connect(ip: String, onReady: (Boolean) -> Unit) {
        val endpoint = ip.trim()
        if (endpoint.isEmpty()) {
            onReady(false)
            return
        }

        socket?.close(1000, "reconnect")
        socket = null
        connected = false

        val request = Request.Builder()
            .url("ws://$endpoint:3000")
            .build()

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                connected = false
                sendRegister()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Mensagem recebida: $text")

                try {
                    val json = JSONObject(text)
                    val payload = json.optJSONObject("payload")

                    if (payload != null && payload.has("client-key")) {
                        val clientKey = payload.getString("client-key")
                        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                        prefs.edit().putString(keyName, clientKey).apply()
                        connected = true
                        Log.d(TAG, "Client-key salvo: $clientKey")
                        onReady(true)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar resposta da TV: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                connected = false
                Log.e(TAG, "Erro de conexão WebSocket: ${t.message}")
                onReady(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected = false
                Log.d(TAG, "WebSocket fechado: $code - $reason")
            }
        })
    }

    private fun getStoredClientKey(): String {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.getString(keyName, "") ?: ""
    }

    private fun sendRegister() {
        if (socket == null) return

        try {
            val manifest = JSONObject().apply {
                put("manifestVersion", 1)
                put("appVersion", "1.1")
                put("permissions", org.json.JSONArray().apply {
                    put("LAUNCH")
                    put("LAUNCH_WEBAPP")
                    put("APP_TO_APP")
                    put("CONTROL_AUDIO")
                    put("CONTROL_DISPLAY")
                    put("CONTROL_INPUT_JOYSTICK")
                    put("CONTROL_INPUT_MEDIA_RECORDING")
                    put("CONTROL_INPUT_MEDIA_PLAYBACK")
                    put("CONTROL_INPUT_TV")
                    put("CONTROL_POWER")
                    put("READ_APP_STATUS")
                    put("READ_CURRENT_CHANNEL")
                    put("READ_INPUT_DEVICE_LIST")
                    put("READ_NETWORK_STATE")
                    put("READ_RUNNING_APPS")
                    put("READ_TV_CHANNEL_LIST")
                    put("WRITE_NOTIFICATION_TOAST")
                    put("READ_POWER_STATE")
                    put("READ_COUNTRY_INFO")
                })
            }

            val message = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("type", "register")
                put("payload", JSONObject().apply {
                    put("manifest", manifest)
                    put("pairingType", "PROMPT")
                    put("client-key", getStoredClientKey())
                })
            }

            socket?.send(message.toString())
            Log.d(TAG, "Registro enviado: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar registro: ${e.message}")
        }
    }

    fun sendCommand(uri: String, payload: Map<String, Any> = emptyMap()): Boolean {
        if (!connected || socket == null) {
            Log.w(TAG, "WebSocket não está conectado")
            return false
        }

        return try {
            val msg = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("type", "request")
                put("uri", uri)

                if (payload.isNotEmpty()) {
                    val payloadObj = JSONObject()
                    for ((key, value) in payload) {
                        payloadObj.put(key, value)
                    }
                    put("payload", payloadObj)
                }
            }

            socket?.send(msg.toString())
            Log.d(TAG, "Comando enviado: $uri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar comando: ${e.message}")
            false
        }
    }

    fun sendKeyAction(key: String): Boolean {
        return when (key) {
            "POWER" -> sendCommand("ssap://system/turnOff")
            "HOME" -> sendCommand("ssap://home/showDashboard")
            "BACK" -> sendCommand("ssap://com.webos.service.ime/sendKeyEvent", mapOf("keyName" to "BACK"))
            "UP" -> sendCommand("ssap://com.webos.service.ime/sendKeyEvent", mapOf("keyName" to "UP"))
            "DOWN" -> sendCommand("ssap://com.webos.service.ime/sendKeyEvent", mapOf("keyName" to "DOWN"))
            "LEFT" -> sendCommand("ssap://com.webos.service.ime/sendKeyEvent", mapOf("keyName" to "LEFT"))
            "RIGHT" -> sendCommand("ssap://com.webos.service.ime/sendKeyEvent", mapOf("keyName" to "RIGHT"))
            "OK" -> sendCommand("ssap://com.webos.service.ime/sendEnterKey")
            "VOLUME_UP" -> sendCommand("ssap://audio/volumeUp")
            "VOLUME_DOWN" -> sendCommand("ssap://audio/volumeDown")
            "MUTE" -> sendCommand("ssap://audio/setMute", mapOf("mute" to true))
            "CHANNEL_UP" -> sendCommand("ssap://tv/channelUp")
            "CHANNEL_DOWN" -> sendCommand("ssap://tv/channelDown")
            "YOUTUBE" -> sendCommand("ssap://system.launcher/launch", mapOf("id" to "youtube.leanback.v4"))
            "NETFLIX" -> sendCommand("ssap://system.launcher/launch", mapOf("id" to "netflix"))
            "INPUT" -> sendCommand("ssap://tv/openInputDevices")
            else -> false
        }
    }

    fun procurarTvLG(): String? {
        for (subnet in subnets) {
            for (i in 1..254) {
                val ip = "$subnet.$i"
                if (canConnectTcp(ip, 3000, 250)) {
                    Log.d(TAG, "TV encontrada em: $ip")
                    return ip
                }
            }
        }
        Log.d(TAG, "Nenhuma TV encontrada na rede local")
        return null
    }

    private fun canConnectTcp(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun disconnect() {
        socket?.close(1000, "desconectar")
        socket = null
        connected = false
    }
}