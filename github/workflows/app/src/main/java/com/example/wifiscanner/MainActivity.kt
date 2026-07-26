package com.example.wifiscanner

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var countText: TextView
    private lateinit var resultsText: TextView
    private lateinit var radarView: RadarView

    private val PERMISSION_REQUEST_CODE = 1001

    // Typical reference RSSI at 1 meter for WiFi (varies by device/router)
    private val TX_POWER_AT_1M = -50.0
    // Path-loss exponent: 2.0 = free space/open area, 2.7-3.0 = indoor with walls
    private val PATH_LOSS_EXPONENT = 2.5

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                displayScanResults()
            } else {
                Toast.makeText(this@MainActivity, "Scan failed, showing last known results", Toast.LENGTH_SHORT).show()
                displayScanResults()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        countText = findViewById(R.id.countText)
        resultsText = findViewById(R.id.resultsText)
        radarView = findViewById(R.id.radarView)

        val scanButton: Button = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            startScan()
        }

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, filter)

        requestPermissionsIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiScanReceiver)
    }

    private fun requestPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            startScan()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan()
            } else {
                Toast.makeText(this, "Location permission is required to scan WiFi networks", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startScan() {
        @Suppress("DEPRECATION")
        val started = wifiManager.startScan()
        if (!started) {
            Toast.makeText(this, "Scan throttled by system, showing last results", Toast.LENGTH_SHORT).show()
            displayScanResults()
        }
    }

    private fun estimateDistanceMeters(rssi: Int): Double {
        // Log-distance path loss model:
        // distance = 10 ^ ((txPower - rssi) / (10 * n))
        return 10.0.pow((TX_POWER_AT_1M - rssi) / (10 * PATH_LOSS_EXPONENT))
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun displayScanResults() {
        val results = wifiManager.scanResults
        countText.text = "Networks found: ${results.size}"

        val sb = StringBuilder()
        val radarPoints = mutableListOf<RadarPoint>()

        val sorted = results.sortedByDescending { it.level }

        for (r in sorted) {
            val ssid = if (r.SSID.isNullOrEmpty()) "(hidden)" else r.SSID
            val rssi = r.level
            val distance = estimateDistanceMeters(rssi)
            sb.append("SSID: $ssid\n")
            sb.append("  Signal: ${rssi} dBm\n")
            sb.append("  Est. distance: ${"%.1f".format(distance)} m\n")
            sb.append("  BSSID: ${r.BSSID}\n\n")

            radarPoints.add(RadarPoint(ssid, distance.toFloat()))
        }

        if (sorted.isEmpty()) {
            sb.append("No networks found yet. Tap 'Scan Now' or wait a moment and try again.")
        }

        resultsText.text = sb.toString()
        radarView.setPoints(radarPoints)
    }
}
