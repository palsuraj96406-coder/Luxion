package com.luxion.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "LUXION"
            textSize = 34f
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "Luxion so raha hai"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 48)
        }

        val startButton = Button(this).apply {
            text = "Start Luxion"
            setOnClickListener { startLuxion() }
        }

        val stopButton = Button(this).apply {
            text = "Stop Luxion"
            setOnClickListener { stopLuxion() }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(startButton)
        layout.addView(stopButton)
        setContentView(layout)
    }

    private fun startLuxion() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
            return
        }

        ContextCompat.startForegroundService(this, Intent(this, LuxionService::class.java))
        status.text = "Luxion jaag raha hai. Bolo: Wake up Luxion"
    }

    private fun stopLuxion() {
        stopService(Intent(this, LuxionService::class.java))
        status.text = "Luxion so gaya"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val micGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (micGranted) {
                startLuxion()
            } else {
                status.text = "Mic permission chahiye, warna Luxion sun nahi paayega"
            }
        }
    }
}
