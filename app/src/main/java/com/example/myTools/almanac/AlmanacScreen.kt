package com.example.myTools.almanac

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myTools.MainActivity
import com.example.myTools.auspicious.AuspiciousQueryScreen
import com.example.myTools.tools.AppSettingsDialog
import com.example.myTools.tools.DataManagementDialog
import com.example.myTools.tools.SupportScreen
import com.example.myTools.ui.BlurryContainer
import com.example.myTools.ui.CommonTopBar
import com.nlf.calendar.Lunar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmanacScreen(modifier: Modifier = Modifier) {
    val today = remember { Date() }
    val lunar = remember { Lunar.fromDate(today) }

    val currentJieQiObj = remember(lunar) { lunar.getPrevJieQi(true) }
    val currentTermName = remember(currentJieQiObj) { currentJieQiObj?.name ?: "" }
    val nextJieQiObj = remember(lunar) { lunar.getNextJieQi(false) }
    val nextTermName = remember(nextJieQiObj) { nextJieQiObj?.name ?: "" }

    val daysSince = remember(currentJieQiObj) {
        if (currentJieQiObj == null) return@remember 0
        val todayCal = Calendar.getInstance().apply {
            time = today
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currTermCal = Calendar.getInstance().apply {
            val s = currentJieQiObj.solar
            set(s.year, s.month - 1, s.day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        ceil((todayCal.timeInMillis - currTermCal.timeInMillis) / (1000.0 * 3600 * 24)).toInt() + 1
    }

    val dayYi = remember(lunar) { lunar.dayYi }
    val dayJi = remember(lunar) { lunar.dayJi }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val zodiacList = remember(currentYear) {
        (-6..6).map { i ->
            val year = currentYear + i
            val l = Lunar.fromYmd(year, 6, 1)
            year to l.yearShengXiao
        }
    }

    val calendarTheme = remember(lunar) {
        val festivals = lunar.festivals + lunar.solar.festivals
        val isSunday = lunar.solar.week == 0
        val isSaturday = lunar.solar.week == 6
        val isSolarTerm = lunar.jieQi.isNotEmpty()
        
        when {
            festivals.any { it.contains("春節") || it.contains("除夕") || it.contains("端午") || it.contains("中秋") || it.contains("國慶") || it.contains("元旦") } -> {
                CardTheme(Color(0xFFB34747), Color.White, Color(0xFFFFD700), "festival")
            }
            isSunday -> {
                CardTheme(Color(0xFFB34747), Color.White, Color(0xFFFFD700), "sunday")
            }
            isSolarTerm -> {
                CardTheme(Color(0xFF436B43), Color.White, Color(0xFFE0F2F1), "term")
            }
            isSaturday -> {
                CardTheme(Color(0xFF436B43), Color.White, Color(0xFFE0F2F1), "saturday")
            }
            else -> {
                CardTheme(Color(0xFF2E455E), Color.White, Color(0xFFB0BEC5), "daily")
            }
        }
    }

    var showTermDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDataManagementDialog by remember { mutableStateOf(false) }
    var showAuspiciousFullScreen by remember { mutableStateOf(false) }
    var showSupportScreen by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val isAnyDialogOpen = showTermDialog || showSettingsDialog || showAuspiciousFullScreen || showSupportScreen

    LaunchedEffect(isAnyDialogOpen) {
        MainActivity.setAppBlurred(isAnyDialogOpen)
    }

    Scaffold(
        topBar = {
            BlurryContainer(isBlur = isAnyDialogOpen) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            text = "黃曆", 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        ) 
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
                                DropdownMenuItem(
                                    text = { Text("打賞支持") },
                                    onClick = {
                                        menuExpanded = false
                                        showSupportScreen = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.VolunteerActivism, null) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        BlurryContainer(
            isBlur = isAnyDialogOpen,
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- 農曆大卡片 ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = calendarTheme.bgColor),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        CalendarPattern(
                            modifier = Modifier.matchParentSize(),
                            patternType = calendarTheme.patternType,
                            color = calendarTheme.textColor.copy(alpha = 0.12f)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${lunar.monthInChinese}月 ${lunar.yearInGanZhi}年",
                                style = MaterialTheme.typography.titleLarge,
                                color = calendarTheme.textColor,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = lunar.dayInChinese,
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Black,
                                color = calendarTheme.textColor,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "【屬${lunar.yearShengXiao}】",
                                style = MaterialTheme.typography.titleMedium,
                                color = calendarTheme.shengXiaoColor,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.TRADITIONAL_CHINESE).format(today),
                                style = MaterialTheme.typography.bodyLarge,
                                color = calendarTheme.textColor.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // --- 宜忌卡片 ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    YiJiRow(title = "宜", items = dayYi, color = Color(0xFFB34747))
                    YiJiRow(title = "忌", items = dayJi, color = Color(0xFF436B43))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- 節氣詳情 ---
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .clickable { showTermDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Eco,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentTermName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(13.dp)
                                ) {
                                    Text(
                                        text = "第${daysSince}天",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                            Text(
                                text = "下一個節氣：$nextTermName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(26.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- 生肖年表 ---
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val screenWidth = maxWidth
                    val itemWidth = 75.dp
                    val density = LocalDensity.current
                    val zodiacListState = rememberLazyListState()
                    val view = LocalView.current

                    LaunchedEffect(zodiacListState) {
                        val tickPx = with(density) { 20.dp.toPx() }
                        val itemWidthPx = with(density) { (75.dp + 10.dp).toPx() }
                        var lastTotalOffset = -1f
                        snapshotFlow { zodiacListState.firstVisibleItemIndex * itemWidthPx + zodiacListState.firstVisibleItemScrollOffset }.collect { currentOffset ->
                            if (lastTotalOffset == -1f) { lastTotalOffset = currentOffset; return@collect }
                            if (kotlin.math.abs(currentOffset - lastTotalOffset) >= tickPx) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                lastTotalOffset = currentOffset
                            }
                        }
                    }

                    LaunchedEffect(key1 = currentYear) {
                        zodiacListState.scrollToItem(index = 6, scrollOffset = -with(density) { ((screenWidth - itemWidth) / 2).toPx().toInt() })
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
                        LazyRow(state = zodiacListState, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(zodiacList, key = { _, (year, _) -> year }) { _, (year, shengXiao) ->
                                ZodiacCard(year = year, shengXiao = shengXiao, isCurrent = year == currentYear)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                ElevatedButton(
                    onClick = { showAuspiciousFullScreen = true },
                    modifier = Modifier.fillMaxWidth(0.85f).height(54.dp),
                    colors = ButtonDefaults.elevatedButtonColors(containerColor = Color(0xFFB34747), contentColor = Color.White),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.EventNote, null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("查看吉日 (Auspicious Days)", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showTermDialog) {
            val structuredData = remember(currentTermName) { SolarTermData.getStructuredData(currentTermName) }
            Dialog(onDismissRequest = { showTermDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Card(modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(12.dp)) {
                    Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "節氣詳解", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = { showTermDialog = false }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Text(text = currentTermName, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (structuredData.isEmpty()) {
                            Text(text = SolarTermData.getDescription(currentTermName), style = MaterialTheme.typography.bodyLarge, lineHeight = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            structuredData.forEach { (title, content) -> SolarTermSection(title = title, content = content); Spacer(modifier = Modifier.height(12.dp)) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showTermDialog = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) { Text("我知道了", style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
        }

        if (showSettingsDialog) AppSettingsDialog(onDismiss = { showSettingsDialog = false })

        if (showDataManagementDialog) DataManagementDialog(onDismiss = { showDataManagementDialog = false })

        if (showSupportScreen) {
            Dialog(
                onDismissRequest = { showSupportScreen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SupportScreen(modifier = Modifier.fillMaxSize())
                        IconButton(
                            onClick = { showSupportScreen = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            }
        }

        if (showAuspiciousFullScreen) {
            Dialog(onDismissRequest = { showAuspiciousFullScreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = Color(0xFFB34747), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "吉日查詢", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFB34747))
                            }
                            IconButton(onClick = { showAuspiciousFullScreen = false }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        AuspiciousQueryScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarPattern(modifier: Modifier = Modifier, patternType: String, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (patternType) {
            "festival" -> {
                val path = Path()
                for (i in 0..3) { for (j in 0..2) { val cx = (i * w / 3); val cy = (j * h / 2); path.moveTo(cx, cy + 20f); path.quadraticTo(cx + 20f, cy, cx + 40f, cy + 20f); path.quadraticTo(cx + 60f, cy + 40f, cx + 80f, cy + 20f) } }
                drawPath(path, color, style = Stroke(width = 2f))
            }
            "term" -> {
                for (i in 0..10) { val x = i * w / 10; drawLine(color, Offset(x, 0f), Offset(x + 20f, h), strokeWidth = 1.5f); for (j in 1..3) { val y = j * h / 4; drawLine(color, Offset(x - 5f, y), Offset(x + 10f, y + 5f), strokeWidth = 2f) } }
            }
            "sunday" -> {
                val step = 40f
                for (x in 0..(w / step).toInt()) { drawLine(color, Offset(x * step, 0f), Offset(x * step, h), strokeWidth = 1f) }
                for (y in 0..(h / step).toInt()) { drawLine(color, Offset(0f, y * step), Offset(w, y * step), strokeWidth = 1f) }
            }
            "saturday" -> {
                val step = 30f
                for (i in -10..30) { drawLine(color, Offset(i * step, 0f), Offset(i * step + h, h), strokeWidth = 1.5f) }
            }
            else -> {
                val path = Path()
                for (j in 0..5) { val y = j * h / 5; path.moveTo(0f, y); for (i in 0..10) { val x = i * w / 10; val offset = if (i % 2 == 0) 15f else -15f; path.quadraticTo(x + w / 20, y + offset, (i + 1) * w / 10, y) } }
                drawPath(path, color, style = Stroke(width = 1f))
            }
        }
    }
}

data class CardTheme(val bgColor: Color, val textColor: Color, val shengXiaoColor: Color, val patternType: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YiJiRow(title: String, items: List<String>, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(46.dp).background(color, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.width(12.dp))
        FlowRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val displayItems = items.take(12)
            if (displayItems.isEmpty()) {
                Text(text = "無", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            } else {
                displayItems.forEach { item ->
                    Text(text = item, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun SolarTermSection(title: String, content: String) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(4.dp, 18.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = content, style = MaterialTheme.typography.bodyMedium, lineHeight = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ZodiacCard(year: Int, shengXiao: String, isCurrent: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(if (isCurrent) 4.dp else 1.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.width(75.dp).then(if (!isCurrent) Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)) else Modifier)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = year.toString(), style = MaterialTheme.typography.labelMedium, color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = shengXiao, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        }
    }
}
