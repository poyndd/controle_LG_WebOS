package com.andre.lgremote

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var client: LgRemoteClient

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        client = LgRemoteClient()

        val ipField = findViewById<EditText>(R.id.ip_field)

        // IP padrão editável
        ipField.setText("192.168.68.59")

        val btnScanTv = findViewById<Button>(R.id.btn_scan_tv)

        val btnPower = findViewById<Button>(R.id.btn_power)
        val btnInput = findViewById<Button>(R.id.btn_input)

        val btnHome = findViewById<Button>(R.id.btn_home)
        val btnBack = findViewById<Button>(R.id.btn_back)

        val btnMute = findViewById<Button>(R.id.btn_mute)

        val btnVolUp = findViewById<Button>(R.id.btn_vol_up)
        val btnVolDown = findViewById<Button>(R.id.btn_vol_down)

        val btnChUp = findViewById<Button>(R.id.btn_ch_up)
        val btnChDown = findViewById<Button>(R.id.btn_ch_down)

        val btnYoutube = findViewById<Button>(R.id.btn_youtube)
        val btnNetflix = findViewById<Button>(R.id.btn_netflix)

        val touchpad = findViewById<View>(R.id.touchpad)

        btnScanTv.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val ipEncontrado = client.procurarTvLG()

                runOnUiThread {
                    if (ipEncontrado != null) {
                        ipField.setText(ipEncontrado)
                        Toast.makeText(
                            this@MainActivity,
                            "TV encontrada: $ipEncontrado",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Nenhuma TV encontrada na rede",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        btnPower.setOnClickListener {
            enviarComando(ipField.text.toString(), "POWER")
        }

        btnInput.setOnClickListener {
            enviarComando(ipField.text.toString(), "INPUT")
        }

        btnHome.setOnClickListener {
            enviarComando(ipField.text.toString(), "HOME")
        }

        btnBack.setOnClickListener {
            enviarComando(ipField.text.toString(), "BACK")
        }

        btnMute.setOnClickListener {
            enviarComando(ipField.text.toString(), "MUTE")
        }

        btnVolUp.setOnClickListener {
            enviarComando(ipField.text.toString(), "VOLUME_UP")
        }

        btnVolDown.setOnClickListener {
            enviarComando(ipField.text.toString(), "VOLUME_DOWN")
        }

        btnChUp.setOnClickListener {
            enviarComando(ipField.text.toString(), "CHANNEL_UP")
        }

        btnChDown.setOnClickListener {
            enviarComando(ipField.text.toString(), "CHANNEL_DOWN")
        }

        btnYoutube.setOnClickListener {
            enviarComando(ipField.text.toString(), "YOUTUBE")
        }

        btnNetflix.setOnClickListener {
            enviarComando(ipField.text.toString(), "NETFLIX")
        }

        var startX = 0f
        var startY = 0f

        touchpad.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                }

                MotionEvent.ACTION_MOVE -> {

                    val dx = event.x - startX
                    val dy = event.y - startY

                    if (dx > 30) {
                        enviarComando(ipField.text.toString(), "RIGHT")
                        startX = event.x
                    }

                    if (dx < -30) {
                        enviarComando(ipField.text.toString(), "LEFT")
                        startX = event.x
                    }

                    if (dy > 30) {
                        enviarComando(ipField.text.toString(), "DOWN")
                        startY = event.y
                    }

                    if (dy < -30) {
                        enviarComando(ipField.text.toString(), "UP")
                        startY = event.y
                    }
                }

                MotionEvent.ACTION_UP -> {
                    enviarComando(ipField.text.toString(), "OK")
                }
            }

            true
        }
    }

    private fun enviarComando(ip: String, comando: String) {

        val endereco = ip.trim()

        if (endereco.isEmpty()) {

            Toast.makeText(
                this,
                "Informe o IP da TV",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        CoroutineScope(Dispatchers.IO).launch {

            val comandoEnviado = client.sendKey(endereco, comando)

            runOnUiThread {

                if (comandoEnviado) {

                    Toast.makeText(
                        this@MainActivity,
                        "Comando enviado: $comando",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this@MainActivity,
                        "Falha ao enviar comando para a TV",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
