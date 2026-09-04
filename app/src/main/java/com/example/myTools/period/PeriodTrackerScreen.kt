package com.example.myTools.period

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    var recordToEdit by remember { mutableStateOf<PeriodRecord?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDataManagementDialog by remember { mutableStateOf(false) }
    var showEducationDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    
    // 用於選擇記錄方式的狀態
    var showActionChoiceDialog by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var isPickingStart by remember { mutableStateOf(true) }
    
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

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

            // 可滑動內容區域 (包含動態跑馬燈、日曆卡片、歷史記錄)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 精簡跑馬燈動態公告欄 (整合預測、生理階段與衛教科普，不佔據大面積空間)
                item {
                    CompactStatusTicker(
                        nextPeriod = nextPeriod,
                        phase = phase,
                        dateFormat = dateFormat,
                        onClick = { showEducationDialog = true }
                    )
                }

                // 2. 日曆圖表與操作按鈕卡片 (核心重點 #1)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                }

                // 3. 歷史記錄標題 (核心重點 #2)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "歷史記錄",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${records.size} 筆記錄",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // 4. 歷史記錄列表
                items(records) { record ->
                    HistoryItem(
                        record = record,
                        dateFormat = dateFormat,
                        onEdit = { recordToEdit = record },
                        onDelete = { recordToDelete = record }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // 修改記錄對話框
    recordToEdit?.let { record ->
        EditPeriodDialog(
            record = record,
            dateFormat = dateFormat,
            onDismiss = { recordToEdit = null },
            onConfirm = { updatedStart, updatedEnd ->
                dataManager.updateRecord(record, PeriodRecord(updatedStart, updatedEnd))
                records = dataManager.getRecords()
                recordToEdit = null
            }
        )
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

    // 月經與安全期科普對話框 (屏佔比 85%)
    if (showEducationDialog) {
        PeriodEducationDialog(onDismiss = { showEducationDialog = false })
    }

    if (showSettingsDialog) {
        AppSettingsDialog(onDismiss = { showSettingsDialog = false })
    }

    if (showDataManagementDialog) {
        DataManagementDialog(onDismiss = { showDataManagementDialog = false })
    }
}

@Composable
fun CompactStatusTicker(
    nextPeriod: Long?,
    phase: Int,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val (phaseName, statusMsg, badgeBg, textColor) = when (phase) {
        1 -> Quadruple(
            "排卵期",
            "當前為排卵期（易孕期），懷孕機率較高！ • 預測下次月經：${nextPeriod?.let { dateFormat.format(Date(it)) } ?: "數據不足"} • 點擊檢視安全期風險與衛教科普",
            Color(0xFFFF9800),
            Color(0xFFE65100)
        )
        2 -> Quadruple(
            "月經期",
            "當前為月經期，請注意休息與保暖。 • 預測下次月經：${nextPeriod?.let { dateFormat.format(Date(it)) } ?: "數據不足"} • 點擊檢視生理衛教科普",
            Color(0xFFF44336),
            Color(0xFFC62828)
        )
        3 -> Quadruple(
            "預測期",
            "預測月經即將到來，請提前做好準備。 • 預測下次月經：${nextPeriod?.let { dateFormat.format(Date(it)) } ?: "數據不足"} • 點擊檢視生理衛教科普",
            Color(0xFFE91E63),
            Color(0xFFAD1457)
        )
        else -> Quadruple(
            "安全期",
            "當前為理論安全期，但仍需注意避孕。 • 預測下次月經：${nextPeriod?.let { dateFormat.format(Date(it)) } ?: "數據不足"} • 點擊檢視安全期風險與衛教科普",
            Color(0xFF4CAF50),
            Color(0xFF2E7D32)
        )
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = badgeBg.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, badgeBg.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = phaseName,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = statusMsg,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun EditPeriodDialog(
    record: PeriodRecord,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit,
    onConfirm: (startDate: Long, endDate: Long?) -> Unit
) {
    var editedStart by remember { mutableLongStateOf(record.startDate) }
    var editedEnd by remember { mutableStateOf(record.endDate) }
    var pickingForStart by remember { mutableStateOf(false) }
    var pickingForEnd by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (pickingForStart) {
        DateTimePickerHandler(
            onDismiss = { pickingForStart = false },
            onDateTimeSelected = { selectedMillis ->
                editedStart = selectedMillis
                pickingForStart = false
                if (editedEnd != null && editedEnd!! < editedStart) {
                    errorMessage = "結束時間不能早於開始時間"
                } else {
                    errorMessage = null
                }
            }
        )
    }

    if (pickingForEnd) {
        DateTimePickerHandler(
            onDismiss = { pickingForEnd = false },
            onDateTimeSelected = { selectedMillis ->
                if (selectedMillis < editedStart) {
                    errorMessage = "結束時間不能早於開始時間"
                } else {
                    editedEnd = selectedMillis
                    errorMessage = null
                }
                pickingForEnd = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改記錄時間") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedCard(
                    onClick = { pickingForStart = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("開始時間", style = MaterialTheme.typography.labelMedium)
                            Text(
                                dateFormat.format(Date(editedStart)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = "修改開始時間")
                    }
                }

                OutlinedCard(
                    onClick = { pickingForEnd = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("結束時間", style = MaterialTheme.typography.labelMedium)
                            Text(
                                editedEnd?.let { dateFormat.format(Date(it)) } ?: "未設定 (進行中)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = "修改結束時間")
                    }
                }

                if (editedEnd != null) {
                    TextButton(
                        onClick = {
                            editedEnd = null
                            errorMessage = null
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("清除結束時間 (設為進行中)")
                    }
                }

                errorMessage?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editedEnd != null && editedEnd!! < editedStart) {
                        errorMessage = "結束時間不能早於開始時間"
                    } else {
                        onConfirm(editedStart, editedEnd)
                    }
                }
            ) {
                Text("儲存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val todayCal = remember { Calendar.getInstance() }

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
                        val cellCal = (currentMonth.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, dayIndex)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val cellDate = cellCal.timeInMillis
                        val isToday = (cellCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                       cellCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR))
                        
                        val cellPhase = dataManager.getCurrentPhase(cellDate, records)
                        
                        CalendarDayCell(
                            day = dayIndex.toString(),
                            phase = cellPhase,
                            isToday = isToday,
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
fun CalendarDayCell(
    day: String,
    phase: Int,
    isToday: Boolean,
    modifier: Modifier
) {
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

    val borderModifier = if (isToday) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(6.dp)
        )
    } else Modifier

    Box(
        modifier = modifier
            .padding(1.dp)
            .height(32.dp) // 固定小高度，節省空間
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .then(borderModifier),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
fun PeriodEducationDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "月經與安全期健康科普",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 第一部分：安全期的限制與風險
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "安全期的限制與風險",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            
                            Text(
                                "• 排卵時間常因外在因素改變：壓力、作息紊亂、情緒、生病或荷爾蒙波動，都可能導致排卵提早或延後。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            
                            Text(
                                "• 週期較短者沒有「前安全期」：若週期為 21～25 天，排卵日可能提前至第 7～10 天，此時經期剛結束甚至經期末期就已進入危險期。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            
                            Text(
                                "• 避孕失敗率高：依據臨床統計，單純依靠安全期推算的年失敗率約為 12%～24%。若無懷孕計畫，建議搭配保險套、事前避孕藥或子宮內避孕器等更可靠的方式。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    // 第二部分：月經的科普
                    Text(
                        "月經的生理科普",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        "月經是女性生理週期的核心表現，也是卵巢與子宮每個月為潛在受孕所進行的循環準備。",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. 月經形成的生理機制
                    EducationSection(
                        title = "月經形成的生理機制",
                        items = listOf(
                            "• 荷爾蒙推動週期：" to "大腦的下視丘與腦下垂體分泌促性腺激素，指揮卵巢發育卵泡並分泌雌激素。",
                            "• 子宮內膜增生：" to "雌激素讓子宮內膜持續增厚、充血，宛如替受精卵準備肥沃的溫床。",
                            "• 排卵與內膜崩解：" to "卵巢釋出一顆成熟卵子。若未受精，體內的黃體素與雌激素濃度會在約兩週後急劇下降，失去荷爾蒙支撐的子宮內膜隨之剝落，混和血液與黏液排出體外，形成月經。"
                        )
                    )

                    // 2. 標準週期的常見數據
                    EducationSection(
                        title = "標準週期的常見數據",
                        items = listOf(
                            "• 週期長度：" to "平均約 28 天（通常 21 至 35 天皆屬正常範圍）。計算方式是從「本次月經見血的第一天」算到「下一次月經見血的前一天」。",
                            "• 出血天數：" to "一般持續 3 至 7 天，量最多的通常是前 2～3 天。",
                            "• 總失血量：" to "整個經期正常失血量約在 30 至 80 毫升 之間（約 2 至 5 湯匙的量），其餘多為剝落的內膜組織與分泌物。"
                        )
                    )

                    // 3. 打破常見的月經迷思
                    EducationSection(
                        title = "打破常見的月經迷思",
                        items = listOf(
                            "• 迷思 1：經血是「排毒」與「壞血」？" to "經血本質上就是普通的血液、脫落的子宮內膜碎片及子宮頸黏液，體內並無毒素藉由經血排出。",
                            "• 迷思 2：經期不能洗頭或運動？" to "洗頭洗澡只要水溫適中、及時吹乾避免受涼即可。適度的輕度有氧運動（如散步、瑜伽）反而能促進骨盆腔血液循環，幫助分泌腦內啡來緩解經痛。",
                            "• 迷思 3：月經期間吃甜食不會變胖？" to "甜食熱量完全相同，並不會因經期消耗更快；大量精緻糖分引起血糖劇烈波動，甚至可能加劇經前情緒不穩或水腫。",
                            "• 迷思 4：經期做愛絕對不會懷孕？" to "精子在女性體內最長可存活 3～5 天。若女性週期較短、排卵提早，或是將排卵期出血誤認為月經，仍有受孕可能。"
                        )
                    )

                    // 4. 經痛與經前不適的原因
                    EducationSection(
                        title = "經痛與經前不適的原因",
                        items = listOf(
                            "• 原發性經痛：" to "主要由子宮內膜釋放的前列腺素過高引起，前列腺素會促使子宮肌肉強烈收縮、造成局部短暫缺血而引發絞痛。非類固醇消炎止痛藥（如布洛芬）可在前列腺素生成前發揮抑制作用。",
                            "• 經前症候群（PMS）：" to "月經來潮前一週內，因荷爾蒙快速變動，常引發下腹悶脹、乳房脹痛、頭痛、情緒焦慮或嗜睡，通常在經血排出後迅速緩解。"
                        )
                    )

                    // 5. 何時應尋求婦產科檢查？
                    EducationSection(
                        title = "何時應尋求婦產科檢查？",
                        items = listOf(
                            "• 月經週期劇烈紊亂：" to "連續數月週期小於 21 天或大於 35 天，或是超過 3 個月沒來（閉經）。",
                            "• 經血過量：" to "每 1～2 小時就需要更換吸飽的衛生棉，或經血中伴隨大量大於 50 元硬幣的血塊，甚至出現頭暈、貧血。",
                            "• 嚴重繼發性經痛：" to "疼痛逐年加劇、止痛藥完全無效，需排除子宮內膜異位症（如巧克力囊腫）或子宮肌腺症。"
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Confirm button at bottom
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("我知道了", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EducationSection(title: String, items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        items.forEach { (subtitle, detail) ->
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
fun HistoryItem(
    record: PeriodRecord,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val today = System.currentTimeMillis()
    val isOngoing = record.endDate == null
    val dayCount = if (isOngoing) {
        (today - record.startDate) / (24 * 60 * 60 * 1000) + 1
    } else {
        (record.endDate - record.startDate) / (24 * 60 * 60 * 1000) + 1
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
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

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "修改", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
