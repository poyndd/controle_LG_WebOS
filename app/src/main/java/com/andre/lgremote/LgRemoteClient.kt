package com.andre.lgremote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.Socket
import java.util.concurrent.TimeUnit

class LgRemoteClient {

    private val http = OkHttpClient.Builder()
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Envia um comando simples para a TV via HTTP.
     * Observação: TVs webOS normalmente usam um protocolo de websocket/JSON para controle.
     * Este método tenta um POST simples em uma rota customizável; adapte conforme a API da sua TV.
     */
    fun sendKey(ip: String, key: String): Boolean {
        return try {
            val url = "http://$ip:3000/remote" // placeholder; adapte para a API real
            val json = """{"key":"$key"}"""
            val body = json.toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url(url)
                .post(body)
                .build()
            val resp = http.newCall(req).execute()
            val success = resp.use { r -> r.isSuccessful }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Método auxiliar para testar conexão TCP (opcional).
     */
    fun canConnectTcp(ip: String, port: Int, timeoutMs: Int = 2000): Boolean {
        return try {
            Socket().use { s ->
                s.connect(java.net.InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
