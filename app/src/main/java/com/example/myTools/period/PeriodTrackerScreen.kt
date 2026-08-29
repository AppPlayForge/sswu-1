package com.example.myTools.period

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myTools.tools.AppSettingsDialog
import com.example.myTools.tools.DataManagementDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerHandler(
    onDismiss: () -> Unit,
    onDateTimeSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    // 先彈出日期選擇
    DisposableEffect(Unit) {
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                // 接著彈出時間選擇
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        onDateTimeSelected(calendar.timeInMillis)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.setOnCancelListener { onDismiss() }
        datePickerDialog.show()
        
        onDispose { }
    }
}

@Composable
fun PeriodTrackerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dataManager = remember { PeriodDataManager(context) }
    var records by remember { mutableStateOf(dataManager.getRecords()) }
    val today = System.currentTimeMillis()
    
    val phase = dataManager.getCurrentPhase(today, records)
    val nextPeriod = dataManager.predictNextPeriod(records)
    
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var recordToDelete by remember { mutableStateOf<PeriodRecord?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDataManagementDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    
    // 用於選擇記錄方式的狀態
    var showActionChoiceDialog by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var isPickingStart by remember { mutableStateOf(true) }
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // 1. 選擇「現在」還是「手動」的對話框
    if (showActionChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showActionChoiceDialog = false },
            title = { Text(if (isPickingStart) "記錄開始時間" else "記錄結束時間") },
            text = { Text("您想要記錄為當前時間，還是手動選擇過去的日期？") },
            confirmButton = {
                Button(onClick = {
                    val now = System.currentTimeMillis()
                    if (isPickingStart) dataManager.addRecord(now) else dataManager.updateLastRecord(now)
                    records = dataManager.getRecords()
                    showActionChoiceDialog = false
                }) {
                    Text("就是現在")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showActionChoiceDialog = false
                    showDateTimePicker = true
                }) {
                    Text("手動選擇日期")
                }
            }
        )
    }

    // 2. 手動日期時間選擇器
    if (showDateTimePicker) {
        DateTimePickerHandler(
            onDismiss = { showDateTimePicker = false },
            onDateTimeSelected = { selectedMillis ->
                if (isPickingStart) {
                    dataManager.addRecord(selectedMillis)
                } else {
                    dataManager.updateLastRecord(selectedMillis)
                }
                records = dataManager.getRecords()
                showDateTimePicker = false
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 自定義頂部導航行 (取代 TopBar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = "月經記錄器", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

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
                            text = { Text("數據管理") },
                            onClick = {
                                menuExpanded = false
                                showDataManagementDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.CloudSync, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("設置") },
                            onClick = {
                                menuExpanded = false
                                showSettingsDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                    }
                }
            }

            // 頂部固定日曆 + 操作按鈕 (緊湊佈局)
            Card(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    PeriodCalendar(
                        currentMonth = currentMonth,
                        records = records,
                        onMonthChange = { currentMonth = it },
                        dataManager = dataManager
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    // 操作區域：按鈕 (佔滿寬度)
                    Box(modifier = Modifier.padding(12.dp)) {
                        ActionButtons(
                            records = records, 
                            onStart = {
                                isPickingStart = true
                                showActionChoiceDialog = true
                            }, 
                            onEnd = {
                                isPickingStart = false
                                showActionChoiceDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 下方可滑動內容
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 預測與提醒
                item {
                    InfoCard(nextPeriod, phase, dateFormat)
                }

                // 安全提醒
                item {
                    ContraceptionReminder()
                }

                // 歷史記錄標題
                item {
                    Text(
                        "歷史記錄",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                items(records) { record ->
                    HistoryItem(record, dateFormat) {
                        recordToDelete = record
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // 刪除確認對話框
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("確認刪除") },
            text = { Text("您確定要刪除這條月經記錄嗎？此操作無法撤銷。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        recordToDelete?.let {
                            dataManager.deleteRecord(it)
                            records = dataManager.getRecords()
                        }
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showSettingsDialog) {
        AppSettingsDialog(onDismiss = { showSettingsDialog = false })
    }

    if (showDataManagementDialog) {
        DataManagementDialog(onDismiss = { showDataManagementDialog = false })
    }
}


@Composable
fun PeriodCalendar(
    currentMonth: Calendar,
    records: List<PeriodRecord>,
    onMonthChange: (Calendar) -> Unit,
    dataManager: PeriodDataManager
) {
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (currentMonth.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday

    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        // 頂部導航
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val prev = currentMonth.clone() as Calendar
                    prev.add(Calendar.MONTH, -1)
                    onMonthChange(prev)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, modifier = Modifier.size(20.dp))
            }
            Text(
                text = monthYearFormat.format(currentMonth.time),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = {
                    val next = currentMonth.clone() as Calendar
                    next.add(Calendar.MONTH, 1)
                    onMonthChange(next)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 星期表頭
        Row(modifier = Modifier.fillMaxWidth()) {
            val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 日期網格
        val totalCells = daysInMonth + firstDayOfWeek
        val rows = (totalCells + 6) / 7
        
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val dayIndex = r * 7 + c - firstDayOfWeek + 1
                    if (dayIndex in 1..daysInMonth) {
                        val cellDate = (currentMonth.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, dayIndex)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        val cellPhase = dataManager.getCurrentPhase(cellDate, records)
                        
                        CalendarDayCell(
                            day = dayIndex.toString(),
                            phase = cellPhase,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        CalendarLegend()
    }
}

@Composable
fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem("月經期", Color(0xFFF44336))
        LegendItem("預測期", Color(0xFFE91E63).copy(alpha = 0.5f))
        LegendItem("排卵期", Color(0xFFFF9800))
        LegendItem("安全期", Color(0xFF4CAF50))
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
    }
}

@Composable
fun CalendarDayCell(day: String, phase: Int, modifier: Modifier) {
    val bgColor = when (phase) {
        0 -> Color(0xFF4CAF50).copy(alpha = 0.1f) // 安全期背景
        1 -> Color(0xFFFF9800).copy(alpha = 0.2f)
        2 -> Color(0xFFF44336).copy(alpha = 0.2f) // 月經期 (紅色背景)
        3 -> Color(0xFFE91E63).copy(alpha = 0.1f) // 預測期
        else -> Color.Transparent
    }
    
    val indicatorColor = when (phase) {
        0 -> Color(0xFF4CAF50) // 安全期指示
        1 -> Color(0xFFFF9800)
        2 -> Color(0xFFF44336) // 月經期 (紅色指示)
        3 -> Color(0xFFE91E63).copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .padding(1.dp)
            .height(32.dp) // 固定小高度，節省空間
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = if (phase != 4) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (phase != 4) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }
        }
    }
}

@Composable
fun ActionButtons(records: List<PeriodRecord>, onStart: () -> Unit, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val isInPeriod = records.isNotEmpty() && records.sortedByDescending { it.startDate }[0].endDate == null
    
    Button(
        onClick = if (isInPeriod) onEnd else onStart,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isInPeriod) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
        )
    ) {
        Text(if (isInPeriod) "結束月經" else "月經來了", fontSize = 14.sp)
    }
}

@Composable
fun InfoCard(nextPeriod: Long?, phase: Int, dateFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("預測下次：${nextPeriod?.let { dateFormat.format(Date(it)) } ?: "數據不足"}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            if (phase == 0) {
                Text("當前為理論安全期，但仍需注意避孕。", color = Color(0xFF4CAF50), fontSize = 12.sp)
            } else if (phase == 1) {
                Text("當前為排卵期，懷孕機率較高。", color = Color(0xFFFF9800), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ContraceptionReminder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "提醒：安全期並非 100% 安全，若無生育計畫，請務必採取可靠的避孕措施。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun HistoryItem(record: PeriodRecord, dateFormat: SimpleDateFormat, onDelete: () -> Unit) {
    val today = System.currentTimeMillis()
    val isOngoing = record.endDate == null
    val dayCount = if (isOngoing) {
        (today - record.startDate) / (24 * 60 * 60 * 1000) + 1
    } else {
        (record.endDate - record.startDate) / (24 * 60 * 60 * 1000) + 1
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOngoing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("開始: ${dateFormat.format(Date(record.startDate))}", style = MaterialTheme.typography.bodySmall)
                Text("結束: ${record.endDate?.let { dateFormat.format(Date(it)) } ?: "進行中"}", style = MaterialTheme.typography.bodySmall)
            }
            
            // 右側顯示天數狀態
            Surface(
                color = if (isOngoing) Color(0xFFF44336) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isOngoing) "第 $dayCount 天" else "共 $dayCount 天",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isOngoing) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
