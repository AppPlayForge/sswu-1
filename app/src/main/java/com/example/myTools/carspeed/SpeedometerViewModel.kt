package com.example.myTools.carspeed

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

//這個 ViewModel 將負責處理獲取速度和衛星資料的邏輯

class SpeedometerViewModel(application: Application) : AndroidViewModel(application) {

    // 直接從單例物件 LocationData 中引用數據流 (StateFlow)。
    // UI 會觀察這些 Flow 的變化來自動更新。
    val speed = LocationData.speed
    val gpsSatellites = LocationData.gpsSatellites  //GPS衛星
    val beidouSatellites = LocationData.beidouSatellites //北斗衛星
    val isRecording = LocationData.isRecording  //正在錄製
    val tripDuration = LocationData.tripDuration  //行程時長
    val maxSpeed = LocationData.maxSpeed
    val averageSpeed = LocationData.averageSpeed  //平均速度
    val tripDistance = LocationData.tripDistance  //行程距離


    //讓 ViewModel 來負責管理 SensorDataManager(感測器數據管理器) 的生命週期
    private val sensorDataManager = SensorDataManager(application)

    //羅盤度數
    val compassDegrees = sensorDataManager.compassDegrees

    // 初始化 AppPreferences
    private val appPreferences = AppPreferences(application)

    // ---管理螢幕恆亮模式的狀態 ---
    // 1. 建立一個私有的、可變的 StateFlow，並從 SharedPreferences 初始化它的值
    private val _wakeLockMode = MutableStateFlow(appPreferences.getWakeLockMode())
    // 2. 暴露一個公開的、只讀的 StateFlow 給 UI
    val wakeLockMode = _wakeLockMode.asStateFlow()
    // ---管理螢幕恆亮模式的狀態 ---

    init {
        // ViewModel 建立時，開始監聽感應器
        sensorDataManager.start()
    }

    override fun onCleared() {
        // ViewModel 被銷毀時，停止監聽，防止記憶體洩漏
        sensorDataManager.stop()
        super.onCleared()
    }

    /**
     * 切換騎行記錄的狀態。
     * 這個函數會根據目前是否正在記錄，來發送不同 action 的 Intent 給 LocationService。
     */
    fun toggleRecording() {
        val intent = Intent(getApplication(), LocationService::class.java)

        if (isRecording.value) {
            intent.action = "STOP"
            getApplication<Application>().startService(intent)
        } else {
            intent.action = "START"
            // 直接呼叫 startForegroundService，因為 minSdk 保證了版本足夠新
            getApplication<Application>().startForegroundService(intent)
        }
    }

    // 一個函數，讓 UI 可以通知我們更新螢幕恆亮模式
    fun setWakeLockMode(newMode: WakeLockMode) {
        _wakeLockMode.value = newMode // 更新 UI 狀態
        appPreferences.saveWakeLockMode(newMode) // 保存到永久儲存
    }
}
