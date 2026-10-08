package com.andre.lgremote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class LgRemoteClient {

    private val http = OkHttpClient.Builder()
        .callTimeout(3, TimeUnit.SECONDS)
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    fun sendKey(ip: String, key: String): Boolean {

        return try {

            val url = "http://$ip:3000/remote"

            val json = """{"key":"$key"}"""

            val body = json.toRequestBody(
                "application/json".toMediaType()
            )

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            http.newCall(request)
                .execute()
                .use { response ->
                    response.isSuccessful
                }

        } catch (e: Exception) {
            false
        }
    }

    fun canConnectTcp(
        ip: String,
        port: Int,
        timeoutMs: Int = 500
    ): Boolean {

        return try {

            Socket().use { socket ->

                socket.connect(
                    InetSocketAddress(ip, port),
                    timeoutMs
                )

                true
            }

        } catch (e: Exception) {
            false
        }
    }

    fun testarTvLG(ip: String): Boolean {

        return canConnectTcp(ip, 3000)
    }

    fun procurarTvLG(): String? {

        val baseRede = "192.168.68"

        for (i in 1..254) {

            val ip = "$baseRede.$i"

            if (canConnectTcp(ip, 3000)) {
                return ip
            }
        }

        return null
    }
}
