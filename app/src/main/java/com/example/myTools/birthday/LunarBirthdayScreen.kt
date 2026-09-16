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
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myTools.utils.AppBadgeManager
import androidx.core.content.ContextCompat
import com.example.myTools.MainActivity
import com.example.myTools.tools.AppSettingsDialog
import com.example.myTools.tools.DataManagementDialog
import com.example.myTools.ui.AppSettingsMenuItem
import com.example.myTools.ui.BlurryContainer
import com.example.myTools.ui.DataManagementMenuItem
import com.example.myTools.ui.DeleteConfirmDialog
import com.example.myTools.ui.SearchableTopBar
import com.example.myTools.ui.ShareAppMenuItem


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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LunarBirthdayScreen(
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    // 持久化數據狀態
    var birthdayList by remember { mutableStateOf(BirthdayManager.loadList(context)) }

    // 刷新全局紅點狀態
    LaunchedEffect(birthdayList) {
        AppBadgeManager.refreshBirthdayBadges(context)
    }

    // 搜索狀態
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(searchQuery, birthdayList) {
        val list = if (searchQuery.isEmpty()) {
            birthdayList
        } else {
            birthdayList.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        list.sortedWith(
            compareBy<BirthdayRecord> {
                getNextBirthdayCalendar(it.lunarMonth, it.lunarDay).timeInMillis
            }.thenBy { it.name }
        )
    }

    // 滾動與 FAB 顯示狀態
    val listState = rememberLazyListState()
    var isFabVisible by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow {
            Triple(listState.isScrollInProgress, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }.collect { (isScrollInProgress, currentIndex, currentOffset) ->
            if (currentIndex == 0 && currentOffset == 0) {
                isFabVisible = true
            } else if (isScrollInProgress) {
                if (currentIndex > previousIndex) {
                    isFabVisible = false
                } else if (currentIndex < previousIndex) {
                    isFabVisible = true
                } else {
                    val diff = currentOffset - previousOffset
                    if (diff > 12) {
                        isFabVisible = false
                    } else if (diff < -12) {
                        isFabVisible = true
                    }
                }
            }
            previousIndex = currentIndex
            previousOffset = currentOffset
        }
    }

    // UI 控制狀態
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDataManagementDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
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
        val hasNotificationPermission = AppBadgeManager.hasNotificationPermission(context)
        val canScheduleExactAlarms = alarmManager.canScheduleExactAlarms()

        if (!hasNotificationPermission || !canScheduleExactAlarms) {
            showPermissionGuide = true
        }
    }

    val isAnyDialogOpen = showPermissionGuide || showAddDialog || showSettingsDialog || showDataManagementDialog || editingRecord != null || recordToDelete != null

    LaunchedEffect(isAnyDialogOpen) {
        MainActivity.setAppBlurred(isAnyDialogOpen)
    }

    DisposableEffect(Unit) {
        onDispose {
            MainActivity.setAppBlurred(false)
        }
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        topBar = {
            BlurryContainer(isBlur = isAnyDialogOpen) {
                SearchableTopBar(
                    title = "農曆生日提醒",
                    isSearchActive = isSearchActive,
                    onSearchActiveChange = { isSearchActive = it },
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (onBack != null) {
                                onBack()
                            } else {
                                backDispatcher?.onBackPressed()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    Icons.Default.MoreVert, 
                                    contentDescription = "更多",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("測試通知") },
                                    onClick = {
                                        menuExpanded = false
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
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = "測試通知"
                                        )
                                    }
                                )
                                DataManagementMenuItem(
                                    onClick = {
                                        menuExpanded = false
                                        showDataManagementDialog = true
                                    }
                                )
                                AppSettingsMenuItem(
                                    onClick = {
                                        menuExpanded = false
                                        showSettingsDialog = true
                                    }
                                )
                                ShareAppMenuItem(
                                    onDismissRequest = { menuExpanded = false }
                                )
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                BlurryContainer(isBlur = isAnyDialogOpen) {
                    Surface(
                        modifier = Modifier
                            .size(96.dp)
                            .combinedClickable(
                                onClick = {
                                    editingRecord = null
                                    showAddDialog = true
                                }
                            ),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "新增",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        BlurryContainer(
            isBlur = isAnyDialogOpen,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (filteredList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val emptyText = if (searchQuery.isEmpty()) "尚未添加生日紀錄" else "未找到匹配 \"$searchQuery\" 的紀錄"
                        Text(
                            text = emptyText, 
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredList, key = { it.id }) { record ->
                            BirthdayCard(
                                record = record,
                                onEdit = { editingRecord = record },
                                onDeleteRequest = { recordToDelete = record }
                            )
                        }
                    }
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
                            style = MaterialTheme.typography.bodyLarge
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
                    onConfirm = { name, month, day, reminds, hour, minute ->
                        val record = if (editingRecord != null) {
                            editingRecord!!.copy(
                                name = name,
                                lunarMonth = month,
                                lunarDay = day,
                                remindList = reminds,
                                remindHour = hour,
                                remindMinute = minute
                            )
                        } else {
                            BirthdayRecord(
                                name = name,
                                lunarMonth = month,
                                lunarDay = day,
                                remindList = reminds,
                                remindHour = hour,
                                remindMinute = minute
                            )
                        }

                        BirthdayManager.addOrUpdateRecord(context, record)
                        birthdayList = BirthdayManager.loadList(context)
                        AppBadgeManager.refreshBirthdayBadges(context)

                        showAddDialog = false
                        editingRecord = null
                    }
                )
            }

            if (showSettingsDialog) {
                AppSettingsDialog(onDismiss = { showSettingsDialog = false })
            }

            if (showDataManagementDialog) {
                DataManagementDialog(onDismiss = { showDataManagementDialog = false })
            }

            // 刪除確認對話框
            if (recordToDelete != null) {
                DeleteConfirmDialog(
                    message = "要刪除 ${recordToDelete!!.name} 的生日提醒嗎？",
                    onDismiss = { recordToDelete = null },
                    onConfirm = {
                        BirthdayManager.deleteRecord(context, recordToDelete!!.id)
                        birthdayList = BirthdayManager.loadList(context)
                        AppBadgeManager.refreshBirthdayBadges(context)
                        recordToDelete = null
                    }
                )
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
