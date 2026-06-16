package com.example.myTools.carspeed

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.myTools.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null
    private val speedDataPoints = mutableListOf<Float>()
    private var durationJob: Job? = null
    private var tripStartTime: Long = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->


                val currentSpeedKmh = location.speed * 3.6f
                LocationData.speed.value = currentSpeedKmh

                // 如果位置物件帶有方向資訊，就更新它
                // location.bearing 只有在裝置正在移動時才會提供準確的值。
                // 當裝置靜止時，這個值可能是 0 或不準確的。這是 GPS 的正常行為。
                if (location.hasBearing()) {
                    LocationData.bearing.value = location.bearing
                }


                if (LocationData.isRecording.value) {
                    LocationData.maxSpeed.value = max(LocationData.maxSpeed.value, currentSpeedKmh)
                    lastLocation?.let {
                        LocationData.tripDistance.value += location.distanceTo(it)
                    }
                    lastLocation = location
                    speedDataPoints.add(currentSpeedKmh)
                    LocationData.averageSpeed.value = speedDataPoints.average().toFloat()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var gpsCount = 0
            var beidouCount = 0
            for (i in 0 until status.satelliteCount) {
                if(status.usedInFix(i)) {
                    when (status.getConstellationType(i)) {
                        GnssStatus.CONSTELLATION_GPS -> gpsCount++
                        GnssStatus.CONSTELLATION_BEIDOU -> beidouCount++
                    }
                }
            }
            LocationData.gpsSatellites.value = gpsCount
            LocationData.beidouSatellites.value = beidouCount
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(LocationManager::class.java)
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startRecording()
            "STOP" -> stopRecordingAndService()
        }
        return START_STICKY // 如果服務被系統殺死，嘗試重啟
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        startForeground(
            NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
        LocationData.isRecording.value = true
        // 重置數據
        LocationData.maxSpeed.value = 0f
        LocationData.averageSpeed.value = 0f
        LocationData.tripDistance.value = 0f
        LocationData.tripDuration.value = 0L
        speedDataPoints.clear()
        lastLocation = null
        tripStartTime = System.currentTimeMillis()

        // 啟動定位更新
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        locationManager.registerGnssStatusCallback(mainExecutor, gnssStatusCallback)

        // 啟動計時器
        durationJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                LocationData.tripDuration.value = (System.currentTimeMillis() - tripStartTime) / 1000
                delay(1000)
            }
        }
    }

    private fun stopRecordingAndService() {
        LocationData.isRecording.value = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        durationJob?.cancel()
        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(): android.app.Notification {
        val channelId = "location_service_channel"
        val channel = NotificationChannel(channelId, "騎行記錄中", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE)
            }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("車速儀記錄中")
            .setContentText("正在背景記錄您的騎行數據")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // 建議換成您自己的 App 圖示
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1
    }
}
