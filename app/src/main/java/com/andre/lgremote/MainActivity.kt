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
    private var isConectado = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        client = LgRemoteClient()

        val ipField = findViewById<EditText>(R.id.ip_field)
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

        // Botão de procurar TV na rede
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
                        
                        // Conecta automaticamente
                        conectarTV(ipEncontrado)
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

        // Botões de controle
        btnPower.setOnClickListener {
            enviarComando("POWER")
        }

        btnInput.setOnClickListener {
            enviarComando("INPUT")
        }

        btnHome.setOnClickListener {
            enviarComando("HOME")
        }

        btnBack.setOnClickListener {
            enviarComando("BACK")
        }

        btnMute.setOnClickListener {
            enviarComando("MUTE")
        }

        btnVolUp.setOnClickListener {
            enviarComando("VOLUME_UP")
        }

        btnVolDown.setOnClickListener {
            enviarComando("VOLUME_DOWN")
        }

        btnChUp.setOnClickListener {
            enviarComando("CHANNEL_UP")
        }

        btnChDown.setOnClickListener {
            enviarComando("CHANNEL_DOWN")
        }

        btnYoutube.setOnClickListener {
            enviarComando("YOUTUBE")
        }

        btnNetflix.setOnClickListener {
            enviarComando("NETFLIX")
        }

        // Touchpad
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
                        enviarComando("RIGHT")
                        startX = event.x
                    }

                    if (dx < -30) {
                        enviarComando("LEFT")
                        startX = event.x
                    }

                    if (dy > 30) {
                        enviarComando("DOWN")
                        startY = event.y
                    }

                    if (dy < -30) {
                        enviarComando("UP")
                        startY = event.y
                    }
                }

                MotionEvent.ACTION_UP -> {
                    enviarComando("OK")
                }
            }
            true
        }

        // Tenta conectar no IP padrão ao iniciar
        conectarTV(ipField.text.toString())
    }

    /**
     * Conecta à TV no IP especificado
     */
    private fun conectarTV(ip: String) {
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
            client.conectarTV(endereco) { conectado ->
                runOnUiThread {
                    isConectado = conectado
                    if (conectado) {
                        Toast.makeText(
                            this@MainActivity,
                            "Conectado à TV em $endereco",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Não foi possível conectar à TV",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    /**
     * Envia comando para a TV
     */
    private fun enviarComando(comando: String) {
        if (!isConectado) {
            Toast.makeText(
                this,
                "Não conectado à TV. Tente conectar novamente.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val sucesso = client.enviarTecla(comando)

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
                        "Erro ao enviar comando",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.desconectar()
    }
}
