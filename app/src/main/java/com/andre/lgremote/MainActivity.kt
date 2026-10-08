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
    private var connected = false
    private var lastTouchActionAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        client = LgRemoteClient(this)

        val ipField = findViewById<EditText>(R.id.ip_field)
        ipField.setText("")

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
            Toast.makeText(this, "Procurando TV na rede...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                val ipEncontrado = client.procurarTvLG()

                runOnUiThread {
                    if (ipEncontrado != null) {
                        ipField.setText(ipEncontrado)
                        Toast.makeText(
                            this@MainActivity,
                            "TV encontrada: $ipEncontrado\nConectando...",
                            Toast.LENGTH_LONG
                        ).show()
                        conectarTV(ipEncontrado)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Nenhuma TV foi encontrada na rede local",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        btnPower.setOnClickListener { enviarComando("POWER") }
        btnInput.setOnClickListener { enviarComando("INPUT") }
        btnHome.setOnClickListener { enviarComando("HOME") }
        btnBack.setOnClickListener { enviarComando("BACK") }
        btnMute.setOnClickListener { enviarComando("MUTE") }
        btnVolUp.setOnClickListener { enviarComando("VOLUME_UP") }
        btnVolDown.setOnClickListener { enviarComando("VOLUME_DOWN") }
        btnChUp.setOnClickListener { enviarComando("CHANNEL_UP") }
        btnChDown.setOnClickListener { enviarComando("CHANNEL_DOWN") }
        btnYoutube.setOnClickListener { enviarComando("YOUTUBE") }
        btnNetflix.setOnClickListener { enviarComando("NETFLIX") }

        var startX = 0f
        var startY = 0f

        touchpad.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                }

                MotionEvent.ACTION_MOVE -> {
                    val now = System.currentTimeMillis()
                    val dx = event.x - startX
                    val dy = event.y - startY

                    if (now - lastTouchActionAt < 120) {
                        return@setOnTouchListener true
                    }

                    if (dx > 30) {
                        enviarComando("RIGHT")
                        startX = event.x
                        lastTouchActionAt = now
                    }

                    if (dx < -30) {
                        enviarComando("LEFT")
                        startX = event.x
                        lastTouchActionAt = now
                    }

                    if (dy > 30) {
                        enviarComando("DOWN")
                        startY = event.y
                        lastTouchActionAt = now
                    }

                    if (dy < -30) {
                        enviarComando("UP")
                        startY = event.y
                        lastTouchActionAt = now
                    }
                }

                MotionEvent.ACTION_UP -> {
                    enviarComando("OK")
                }
            }
            true
        }
    }

    private fun conectarTV(ip: String) {
        val endereco = ip.trim()
        if (endereco.isEmpty()) {
            Toast.makeText(this, "Informe o IP da TV", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            client.connect(endereco) { ok ->
                runOnUiThread {
                    connected = ok
                    if (ok) {
                        Toast.makeText(
                            this@MainActivity,
                            "Conectado à TV em $endereco. Aceite o prompt na TV.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Não foi possível conectar à TV. Verifique o IP e a rede.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun enviarComando(comando: String) {
        if (!connected) {
            Toast.makeText(
                this,
                "Não conectado à TV. Primeiro procure ou conecte à TV.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val sucesso = client.sendKeyAction(comando)

            runOnUiThread {
                if (sucesso) {
                    Toast.makeText(
                        this@MainActivity,
                        "Comando enviado: $comando",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Erro ao enviar comando para a TV",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.disconnect()
    }
}
