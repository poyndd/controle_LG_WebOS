package com.andre.lgremote

import android.os.Bundle
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
        val btnPower = findViewById<Button>(R.id.btn_power)
        val btnVolUp = findViewById<Button>(R.id.btn_vol_up)
        val btnVolDown = findViewById<Button>(R.id.btn_vol_down)

        btnPower.setOnClickListener {
            val ip = ipField.text.toString().trim()
            if (ip.isEmpty()) {
                Toast.makeText(this, "Informe o IP da TV", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                val ok = client.sendKey(ip, "POWER")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, if (ok) "Comando enviado" else "Falha", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnVolUp.setOnClickListener {
            val ip = ipField.text.toString().trim()
            CoroutineScope(Dispatchers.IO).launch {
                val ok = client.sendKey(ip, "VOLUME_UP")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, if (ok) "Volume + enviado" else "Falha", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnVolDown.setOnClickListener {
            val ip = ipField.text.toString().trim()
            CoroutineScope(Dispatchers.IO).launch {
                val ok = client.sendKey(ip, "VOLUME_DOWN")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, if (ok) "Volume - enviado" else "Falha", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
