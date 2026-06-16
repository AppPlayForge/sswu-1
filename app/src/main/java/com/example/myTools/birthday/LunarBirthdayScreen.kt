package com.example.myTools.birthday

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myTools.ui.DeleteConfirmDialog


/*
* 1.BirthdayRecord.kt：純數據模型。
2.BirthdayUtils.kt：包含農曆名稱轉換及日期計算邏輯。
3.BirthdayManager.kt：負責數據持久化（SharedPreferences）及鬧鐘管理。
4.BirthdayCard.kt：獨立的列表卡片組件，支援長按修改與刪除確認。
5.AddBirthdayDialog.kt：獨立的對話框組件，支援新增與編輯模式。
6.LunarBirthdayScreen.kt：主頁面入口，負責組合以上所有模組。
* */


/**
 * 農曆生日提醒主頁面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarBirthdayScreen() {
    val context = LocalContext.current
    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    // 持久化數據狀態
    var birthdayList by remember { mutableStateOf(BirthdayManager.loadList(context)) }

    // UI 控制狀態
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<BirthdayRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<BirthdayRecord?>(null) }
    var showPermissionGuide by remember { mutableStateOf(false) }

    // 權限請求啟動器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "通知權限已開啟", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "未開啟通知權限，可能無法收到生日提醒", Toast.LENGTH_LONG).show()
        }
    }

    // 檢查並提示權限
    LaunchedEffect(Unit) {
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val canScheduleExactAlarms = alarmManager.canScheduleExactAlarms()

        if (!hasNotificationPermission || !canScheduleExactAlarms) {
            showPermissionGuide = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("農曆生日提醒", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        // 檢查通知權限 (Android 13+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            when (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            )) {
                                PackageManager.PERMISSION_GRANTED -> {
                                    sendTestNotification(context)
                                }

                                else -> {
                                    // 彈出權限申請
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        } else {
                            // Android 13 以下版本通常預設開啟或由系統管理
                            sendTestNotification(context)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "測試通知",
                            tint = Color(0xFFFF5722)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRecord = null
                    showAddDialog = true
                },
                containerColor = Color(0xFFFF5722),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增")
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (birthdayList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("尚未添加生日紀錄", color = Color.Gray, fontSize = 18.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(birthdayList, key = { it.id }) { record ->
                        BirthdayCard(
                            record = record,
                            onEdit = { editingRecord = record },
                            onDeleteRequest = { recordToDelete = record }
                        )
                    }
                }
            }

            // 權限引導對話框
            if (showPermissionGuide) {
                AlertDialog(
                    onDismissRequest = { showPermissionGuide = false },
                    title = { Text("需要必要權限") },
                    text = {
                        Text(
                            "為了確保能準時收到生日提醒，請開啟「通知」和「精確鬧鐘」權限。",
                            fontSize = 18.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showPermissionGuide = false
                                // 請求通知權限
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                // 請求精確鬧鐘權限 (跳轉設定頁)
                                if (!alarmManager.canScheduleExactAlarms()) {
                                    val intent =
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data =
                                                Uri.fromParts("package", context.packageName, null)
                                        }
                                    context.startActivity(intent)
                                }
                            }
                        ) {
                            Text("去開啟", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionGuide = false }) {
                            Text("稍後再說")
                        }
                    }
                )
            }

            // 新增或編輯對話框
            if (showAddDialog || editingRecord != null) {
                AddBirthdayDialog(
                    initialRecord = editingRecord,
                    onDismiss = {
                        showAddDialog = false
                        editingRecord = null
                    },
                    onConfirm = { name, month, day, reminds, hours ->
                        val record = if (editingRecord != null) {
                            editingRecord!!.copy(
                                name = name,
                                lunarMonth = month,
                                lunarDay = day,
                                remindList = reminds,
                                remindHours = hours
                            )
                        } else {
                            BirthdayRecord(
                                name = name,
                                lunarMonth = month,
                                lunarDay = day,
                                remindList = reminds,
                                remindHours = hours
                            )
                        }

                        BirthdayManager.addOrUpdateRecord(context, record)
                        birthdayList = BirthdayManager.loadList(context)

                        showAddDialog = false
                        editingRecord = null
                    }
                )
            }

            // 刪除確認對話框
            if (recordToDelete != null) {
                DeleteConfirmDialog(
                    message = "要刪除 ${recordToDelete!!.name} 的生日提醒嗎？",
                    onDismiss = { recordToDelete = null },
                    onConfirm = {
                        BirthdayManager.deleteRecord(context, recordToDelete!!.id)
                        birthdayList = BirthdayManager.loadList(context)
                        recordToDelete = null
                    }
                )
            }
        }
    }
}

/**
 * 發送測試通知的輔助方法
 */
private fun sendTestNotification(context: Context) {
    val intent = Intent(context, BirthdayReceiver::class.java).apply {
        putExtra("name", "測試提醒")
        putExtra("message", "通知功能測試成功！🎉")
        putExtra("id", 999)
    }
    context.sendBroadcast(intent)
}
