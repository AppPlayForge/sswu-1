package com.example.myTools.carspeed

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myTools.MainActivity

@Composable
fun CarSpeedScreen(onBack: () -> Unit) {
    val viewModel: SpeedometerViewModel = viewModel()
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }

    // 建立一個新的、可以請求定位權限的啟動器
    val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    //State 來控制是否顯示提示對話框
    val showPermissionDialog = remember { mutableStateOf(false) }
    val showGpsDialog = remember { mutableStateOf(false) }

    // 檢查系統 GPS 是否開啓
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun checkGpsAndToggle() {
        if (isLocationEnabled(context)) {
            viewModel.toggleRecording()
        } else {
            showGpsDialog.value = true
        }
    }

    val multiplePermissionResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineLocationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineLocationGranted) {
            checkGpsAndToggle()
        } else {
            showPermissionDialog.value = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF101828) // 使用與羅盤一致的深色背景
    ) {
        
        // 從 SharedPreferences 讀取初始狀態
        val showHelpDialog = remember { mutableStateOf(appPreferences.shouldShowHelpDialog()) }

        // 當 showPermissionDialog 為 true 時，顯示 AlertDialog
        if (showPermissionDialog.value) {
            PermissionAlertDialog(
                onDismiss = { showPermissionDialog.value = false },
                onConfirm = {
                    showPermissionDialog.value = false
                    // 引導用戶去設定頁面
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                }
            )
        }

        if (showGpsDialog.value) {
            GpsAlertDialog(
                onDismiss = { showGpsDialog.value = false },
                onConfirm = {
                    showGpsDialog.value = false
                    // 引導用戶去系統定位設定
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                }
            )
        }

        SpeedometerScreen(
            viewModel = viewModel,
            onBack = onBack,
            onToggleRecording = {
                if (viewModel.isRecording.value) {
                    // 如果正在錄製，直接停止，不需要檢查權限或 GPS
                    viewModel.toggleRecording()
                } else {
                    // 如果要開始錄製，才檢查權限與 GPS
                    val hasFineLocationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasFineLocationPermission) {
                        checkGpsAndToggle()
                    } else {
                        // 直接發起系統請求
                        multiplePermissionResultLauncher.launch(permissionsToRequest)
                    }
                }
            },
            showHelpDialog = showHelpDialog.value,
            onHelpDialogDismissed = {
                // 當 UI 通知我們關閉時，更新 UI 狀態並寫入永久儲存
                showHelpDialog.value = false
                appPreferences.setHelpDialogShown()
            }
        )
    }
}

//一個用於顯示權限提示的 Composable 函數
@Composable
fun PermissionAlertDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "需要定位權限") },
        text = { Text(text = "需要精確的定位權限才能計算時速和里程。請手動開啟權限。") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("前往設定")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun GpsAlertDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "GPS 未開啟") },
        text = { Text(text = "偵測到您的系統 GPS 尚未開啟，請開啟 GPS 以獲得準確的速度數據。") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("前往開啟")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
