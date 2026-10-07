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
        .build()

    fun sendKey(ip: String, key: String): Boolean {

        return try {

            val url = "http://$ip:3000/remote"

            val json = """{"key":"$key"}"""

            val body = json.toRequestBody(
                "application/json".toMediaType()
            )

            val req = Request.Builder()
                .url(url)
                .post(body)
                .build()

            http.newCall(req)
                .execute()
                .use { it.isSuccessful }

        } catch (e: Exception) {

            false
        }
    }

    fun canConnectTcp(
        ip: String,
        port: Int,
        timeoutMs: Int = 1000
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

    fun procurarTvNaRede(
        baseRede: String
    ): String? {

        for (i in 1..254) {

            val ip = "$baseRede.$i"

            val abriu3000 = canConnectTcp(
                ip,
                3000,
                300
            )

            val abriu3001 = canConnectTcp(
                ip,
                3001,
                300
            )

            if (abriu3000 || abriu3001) {
                return ip
            }
        }

        return null
    }
}
