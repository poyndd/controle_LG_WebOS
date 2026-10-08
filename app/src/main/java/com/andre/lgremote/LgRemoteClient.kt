package com.andre.lgremote

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

class LgRemoteClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var clientKey: String? = null
    private val TAG = "LgRemoteClient"

    private val subnets = listOf(
        "192.168.0",
        "192.168.1",
        "192.168.68",
        "10.0.0",
        "172.16.0"
    )

    fun conectarTV(ip: String, onConnected: (Boolean) -> Unit) {
        val endereco = ip.trim()
        if (endereco.isEmpty()) {
            onConnected(false)
            return
        }

        webSocket?.close(1000, "reconnect")
        webSocket = null
        isConnected = false

        val request = Request.Builder()
            .url("ws://$endereco:3000")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d(TAG, "WebSocket conectado em $endereco")
                isConnected = true
                enviarRegistro()
                onConnected(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Mensagem recebida: $text")
                try {
                    val json = JSONObject(text)
                    if (json.has("payload")) {
                        val payload = json.optJSONObject("payload")
                        if (payload != null && payload.has("client-key")) {
                            clientKey = payload.getString("client-key")
                            Log.d(TAG, "Client-key recebido: $clientKey")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar resposta da TV: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "Erro de WebSocket: ${t.message}")
                isConnected = false
                onConnected(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket fechado: $code - $reason")
                isConnected = false
            }
        }

        webSocket = http.newWebSocket(request, listener)
    }

    private fun enviarRegistro() {
        try {
            val manifest = JSONObject().apply {
                put("manifestVersion", 1)
                put("appVersion", "1.1")
                put("signed", JSONObject().apply {
                    put("created", "20140509")
                    put("appId", "com.lge.test")
                    put("vendorId", "com.lge")
                    put("localizedAppNames", JSONObject().apply { put("", "LG Remote App") })
                    put("localizedVendorNames", JSONObject().apply { put("", "LG Electronics") })
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
                    put("serial", UUID.randomUUID().toString().replace("-", ""))
                })
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
                put("signatures", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("signatureVersion", 1)
                        put("signature", "signature")
                    })
                })
            }

            val payload = JSONObject().apply {
                put("type", "register")
                put("id", UUID.randomUUID().toString())
                put("payload", JSONObject().apply {
                    put("manifest", manifest)
                    put("client-key", clientKey ?: "")
                })
            }

            webSocket?.send(payload.toString())
            Log.d(TAG, "Registro enviado para a TV")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar registro: ${e.message}")
        }
    }

    fun enviarComando(uri: String, payload: Map<String, Any> = emptyMap()): Boolean {
        if (!isConnected || webSocket == null) {
            Log.w(TAG, "WebSocket não está conectado")
            return false
        }

        return try {
            val comando = JSONObject().apply {
                put("type", "request")
                put("id", UUID.randomUUID().toString())
                put("uri", uri)
                if (payload.isNotEmpty()) {
                    val payloadJson = JSONObject()
                    payload.forEach { (key, value) -> payloadJson.put(key, value) }
                    put("payload", payloadJson)
                }
            }

            webSocket?.send(comando.toString())
            Log.d(TAG, "Comando enviado: $uri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar comando: ${e.message}")
            false
        }
    }

    fun enviarTecla(nomeTecla: String): Boolean {
        return when (nomeTecla) {
            "POWER" -> enviarComando("ssap://system/turnOff")
            "HOME" -> enviarComando("ssap://home/showDashboard")
            "BACK" -> enviarComando("ssap://com.webos.service.ime/sendKeyEvent", mapOf("keyName" to "BACK"))
            "UP" -> enviarComando("ssap://com.webos.service.ime/sendKeyEvent", mapOf("keyName" to "UP"))
            "DOWN" -> enviarComando("ssap://com.webos.service.ime/sendKeyEvent", mapOf("keyName" to "DOWN"))
            "LEFT" -> enviarComando("ssap://com.webos.service.ime/sendKeyEvent", mapOf("keyName" to "LEFT"))
            "RIGHT" -> enviarComando("ssap://com.webos.service.ime/sendKeyEvent", mapOf("keyName" to "RIGHT"))
            "OK" -> enviarComando("ssap://com.webos.service.ime/sendEnterKey")
            "VOLUME_UP" -> enviarComando("ssap://audio/volumeUp")
            "VOLUME_DOWN" -> enviarComando("ssap://audio/volumeDown")
            "MUTE" -> enviarComando("ssap://audio/setMute", mapOf("mute" to true))
            "CHANNEL_UP" -> enviarComando("ssap://tv/channelUp")
            "CHANNEL_DOWN" -> enviarComando("ssap://tv/channelDown")
            "YOUTUBE" -> enviarComando("ssap://system.launcher/launch", mapOf("id" to "youtube.leanback.v4"))
            "NETFLIX" -> enviarComando("ssap://system.launcher/launch", mapOf("id" to "netflix"))
            "INPUT" -> enviarComando("ssap://tv/openInputDevices")
            else -> false
        }
    }

    fun testarConexaoTCP(ip: String, timeout: Int = 500): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, 3000), timeout)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun procurarTvLG(): String? {
        for (subnet in subnets) {
            for (i in 1..254) {
                val ip = "$subnet.$i"
                if (testarConexaoTCP(ip, 250)) {
                    Log.d(TAG, "TV encontrada em: $ip")
                    return ip
                }
            }
        }
        Log.d(TAG, "Nenhuma TV encontrada na rede")
        return null
    }

    fun desconectar() {
        webSocket?.close(1000, "desconectar")
        isConnected = false
        webSocket = null
    }
}
