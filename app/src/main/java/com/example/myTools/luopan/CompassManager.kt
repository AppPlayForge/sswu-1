package com.example.myTools.luopan

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class CompassData(
    val azimuth: Float,
    val accuracy: Int
)

class CompassManager(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // 濾波系數：越小越平滑，越大越靈敏 (0.0~1.0)
    private val alpha = 0.25f
    private var lastAzimuth = -1f

    val compassFlow: Flow<CompassData> = callbackFlow {
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null
        var currentAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity = event.values
                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = event.values

                if (gravity != null && geomagnetic != null) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        
                        var degree = (Math.toDegrees(orientation[0].toDouble()) + 360).toFloat() % 360
                        
                        // 簡單的低通濾波與 0/360 度跨越處理
                        if (lastAzimuth < 0) {
                            lastAzimuth = degree
                        } else {
                            var diff = degree - lastAzimuth
                            if (diff > 180) diff -= 360
                            else if (diff < -180) diff += 360
                            lastAzimuth += alpha * diff
                        }
                        
                        trySend(CompassData((lastAzimuth + 360) % 360, currentAccuracy))
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) currentAccuracy = accuracy
            }
        }

        // 使用 SENSOR_DELAY_UI (約 60fps) 對於羅盤已足夠，且更省電穩定
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
