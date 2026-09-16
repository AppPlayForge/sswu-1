package com.example.myTools.carspeed

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 這個 ViewModel 負責處理獲取速度、衛星資料以及感測器邏輯

class SpeedometerViewModel(application: Application) : AndroidViewModel(application) {

    // 將單例物件 LocationData 中的數據流轉換為只讀 StateFlow 暴露給 UI，確保單向資料流與封裝性
    val speed: StateFlow<Float> = LocationData.speed.asStateFlow()
    val gpsSatellites: StateFlow<Int> = LocationData.gpsSatellites.asStateFlow() // GPS 衛星數
    val beidouSatellites: StateFlow<Int> = LocationData.beidouSatellites.asStateFlow() // 北斗衛星數
    val isRecording: StateFlow<Boolean> = LocationData.isRecording.asStateFlow() // 正在錄製
    val tripDuration: StateFlow<Long> = LocationData.tripDuration.asStateFlow() // 行程時長
    val maxSpeed: StateFlow<Float> = LocationData.maxSpeed.asStateFlow()
    val averageSpeed: StateFlow<Float> = LocationData.averageSpeed.asStateFlow() // 平均速度
    val tripDistance: StateFlow<Float> = LocationData.tripDistance.asStateFlow() // 行程距離

    // 管理 SensorDataManager (感測器數據管理器) 的生命週期
    private val sensorDataManager = SensorDataManager(application)

    // 羅盤度數
    val compassDegrees = sensorDataManager.compassDegrees

    // 初始化 AppPreferences
    private val appPreferences = AppPreferences(application)

    // 管理螢幕恆亮模式的狀態
    private val _wakeLockMode = MutableStateFlow(appPreferences.getWakeLockMode())
    val wakeLockMode: StateFlow<WakeLockMode> = _wakeLockMode.asStateFlow()

    init {
        // ViewModel 建立時，開始監聽感應器
        sensorDataManager.start()
    }

    override fun onCleared() {
        // ViewModel 被銷毀時，停止監聽，防止記憶體洩漏
        sensorDataManager.stop()
    }

    /**
     * 切換騎行記錄的狀態。
     * 根據目前是否正在記錄，發送不同 action 的 Intent 給 LocationService。
     */
    fun toggleRecording() {
        val app = getApplication<Application>()
        val intent = Intent(app, LocationService::class.java).apply {
            action = if (isRecording.value) "STOP" else "START"
        }

        if (isRecording.value) {
            app.startService(intent)
        } else {
            app.startForegroundService(intent)
        }
    }

    /**
     * 讓 UI 可以通知更新螢幕恆亮模式
     */
    fun setWakeLockMode(newMode: WakeLockMode) {
        _wakeLockMode.value = newMode // 更新 UI 狀態
        appPreferences.saveWakeLockMode(newMode) // 保存到永久儲存
    }
}
