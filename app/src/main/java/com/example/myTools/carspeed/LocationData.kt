package com.example.myTools.carspeed

import kotlinx.coroutines.flow.MutableStateFlow


//為了讓 Service 和我們的 UI (ViewModel) 之間可以方便地傳遞數據，我們建立一個單例 (Singleton) 物件

object LocationData {
    val speed = MutableStateFlow(0f)
    val tripDistance = MutableStateFlow(0f)  //行程距離
    val maxSpeed = MutableStateFlow(0f)
    val averageSpeed = MutableStateFlow(0f)  //平均速度
    val tripDuration = MutableStateFlow(0L)  //行程時長
    val isRecording = MutableStateFlow(false)  //正在錄製
    val gpsSatellites = MutableStateFlow(0)
    val beidouSatellites = MutableStateFlow(0)  //北斗衛星

    //儲存 GPS 提供的移動方向角度
    val bearing = MutableStateFlow(0f)
}
