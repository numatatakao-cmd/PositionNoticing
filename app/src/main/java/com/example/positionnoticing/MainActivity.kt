package com.example.positionnoticing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.positionnoticing.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 🛰️ 本物の位置情報を取得するための「GPSセンサーの操作役」を準備
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            Toast.makeText(this, "位置情報が許可されました", Toast.LENGTH_SHORT).show()
            // ⭕ 許可されたので、本物の位置情報を取得する
            getRealLocation()
        } else {
            Toast.makeText(this, "位置情報の許可が必要です", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🛰️ GPSセンサーの操作役を初期化（ここで lateinit の約束を果たします）
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnSendLocation.setOnClickListener {
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // ⭕ すでに許可されているので、本物の位置情報を取得する
            getRealLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 🎯 【新機能】スマホのGPSから本物の緯度・経度を取得する関数
    @SuppressLint("MissingPermission")
    private fun getRealLocation() {
        // スマホが最後に記録した最新の位置情報を引っ張ってくる
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // 本物の緯度（Latitude）と経度（Longitude）を取り出す
                val latitude = location.latitude
                val longitude = location.longitude

                // 🗺️ 本物の緯度・経度を埋め込んだ GoogleマップのURLを組み立てる！
                val realMapUrl = "https://www.google.com/maps?q=$latitude,$longitude"

                // メールアプリに本物のURLを渡して起動！
                sendEmail(realMapUrl)
            } else {
                // 部屋の奥深くなどでGPSがどうしても受信できなかった場合
                Toast.makeText(
                    this,
                    "位置情報が取得できませんでした。少し移動して試してください。",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun sendEmail(mapUrl: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "私の現在地です")
            putExtra(Intent.EXTRA_TEXT, "ここにいます。地図で確認してください：\n$mapUrl")
        }
        startActivity(intent)
    }
}