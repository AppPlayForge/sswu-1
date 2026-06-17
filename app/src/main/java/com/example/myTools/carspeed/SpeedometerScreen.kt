package com.example.myTools.carspeed

import android.Manifest
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myTools.MainActivity
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


// 定義螢幕恆亮的三種狀態
enum class WakeLockMode {
    OFF, TEN_MINUTES, ALWAYS
}


@Composable
fun SpeedometerScreen(
    viewModel: SpeedometerViewModel,
    onToggleRecording: () -> Unit,
    showHelpDialog: Boolean,   // << 接收一個 Boolean
    onHelpDialogDismissed: () -> Unit,  // << 接收一個回呼
    onBack: () -> Unit // << 新增返回回呼
) {
    // 從 viewModel 收集數據
    val speed by viewModel.speed.collectAsStateWithLifecycle()
    val gpsSatellites by viewModel.gpsSatellites.collectAsStateWithLifecycle()
    val beidouSatellites by viewModel.beidouSatellites.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val tripDuration by viewModel.tripDuration.collectAsStateWithLifecycle()
    val maxSpeed by viewModel.maxSpeed.collectAsStateWithLifecycle()
    val averageSpeed by viewModel.averageSpeed.collectAsStateWithLifecycle()
    val tripDistance by viewModel.tripDistance.collectAsStateWithLifecycle()
    val compassDegrees by viewModel.compassDegrees.collectAsStateWithLifecycle()
    // val bearingDegrees by viewModel.bearing.collectAsStateWithLifecycle() // 獲取移動方向 (目前沒用到)

    // 螢幕恆亮的狀態
    val wakeLockMode by viewModel.wakeLockMode.collectAsStateWithLifecycle() // << 從 ViewModel 收集狀態
    val context = LocalContext.current


    // 當需要顯示對話框時，調用我們新建的 Composable
    if (showHelpDialog) {
        FeatureHelpDialog(onDismiss = onHelpDialogDismissed) // << 呼叫傳入的回呼
    }

    // 螢幕恆亮邏輯
    LaunchedEffect(wakeLockMode) {
        val activity = context as? MainActivity ?: return@LaunchedEffect
        when (wakeLockMode) {
            WakeLockMode.OFF -> {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            WakeLockMode.ALWAYS -> {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            WakeLockMode.TEN_MINUTES -> {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                delay((10 * 60 * 1000L).milliseconds) // 等待 10 分鐘
                // <<< 關鍵修改 2：不要直接修改狀態，而是通知 ViewModel
                // wakeLockMode = WakeLockMode.OFF // << 刪除或註解掉這一行錯誤的程式碼
                if (viewModel.wakeLockMode.value == WakeLockMode.TEN_MINUTES) {
                    viewModel.setWakeLockMode(WakeLockMode.OFF) // << 使用這一行正確的程式碼
                }
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.padding(16.dp))
        // 頂部控制欄：包含返回按鈕與螢幕恆亮按鈕
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按鈕
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }

            // 螢幕恆亮狀態與按鈕
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeWakeLockColor = Color(0xFFFBC02D) // 定義一個醒目的黃色

                Text(
                    text = when (wakeLockMode) {
                        WakeLockMode.OFF -> "關閉"
                        WakeLockMode.TEN_MINUTES -> "10分鐘"
                        WakeLockMode.ALWAYS -> "常亮"
                    },
                    fontSize = 12.sp,
                    color = Color.White
                )

                IconButton(
                    onClick = {
                        val nextMode = when (wakeLockMode) {
                            WakeLockMode.OFF -> WakeLockMode.TEN_MINUTES
                            WakeLockMode.TEN_MINUTES -> WakeLockMode.ALWAYS
                            WakeLockMode.ALWAYS -> WakeLockMode.OFF
                        }
                        viewModel.setWakeLockMode(nextMode) // << 通知 ViewModel
                    }) {

                    // 根據狀態決定圖示的顏色
                    val iconColor = when (wakeLockMode) {
                        WakeLockMode.OFF -> Color.White.copy(alpha = 0.6f)
                        else -> activeWakeLockColor
                    }

                    Icon(
                        // 根據狀態決定使用實心圖示還是空心圖示
                        imageVector = if (wakeLockMode != WakeLockMode.OFF) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                        contentDescription = "螢幕恆亮",
                        tint = iconColor // << 應用計算出的顏色
                    )
                }
            }
        }

        Spacer(modifier = Modifier.padding(16.dp))

        // --- 指針式儀表板 ---
        SpeedometerGauge(
            modifier = Modifier
                .fillMaxWidth(0.9f) // 佔據寬度的 90%
                .aspectRatio(1f),   // 保持正方形
            speed = speed
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 衛星數量與指南針 (科技感排版)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                SatelliteInfoRow("GPS", gpsSatellites, Color(0xFF00E5FF))
                Spacer(modifier = Modifier.height(4.dp))
                SatelliteInfoRow("北斗", beidouSatellites, Color(0xFF2979FF))
            }
            
            // 指南針
            CompassGauge(
                modifier = Modifier.size(60.dp),
                degrees = compassDegrees
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 開始/停止記錄按鈕 (科技感漸變)
        val buttonBrush = if (isRecording) {
            Brush.horizontalGradient(listOf(Color(0xFFFF5252), Color(0xFFFF1744)))
        } else {
            Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF2979FF)))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(buttonBrush)
                .clickable {
                    onToggleRecording()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRecording) "停止記錄" else "開始記錄",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 顯示記錄數據的卡片
        if (tripDuration > 0 || isRecording) {
            TripDataCard(
                duration = tripDuration,
                maxSpeed = maxSpeed,
                averageSpeed = averageSpeed,
                distance = tripDistance
            )
        } else {
            // 增加一個佔位符，避免按鈕跳動
            Spacer(modifier = Modifier.height(140.dp))
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun SatelliteInfoRow(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        Text(
            text = count.toString(),
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
