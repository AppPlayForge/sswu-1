package com.example.myTools.almanac

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myTools.auspicious.AuspiciousQueryScreen
import com.example.myTools.tools.AppSettingsDialog
import com.example.myTools.ui.CommonTopBar
import com.nlf.calendar.Lunar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@Composable
fun AlmanacScreen(modifier: Modifier = Modifier) {
    val today = remember { Date() }
    val lunar = remember { Lunar.fromDate(today) }

    // 使用 remember 快取計算結果，避免每次重組都重新計算
    val currentJieQiObj = remember(lunar) { lunar.getPrevJieQi(false) }
    val currentTermName = remember(currentJieQiObj) { currentJieQiObj?.name ?: "" }
    val nextJieQiObj = remember(lunar) { lunar.getNextJieQi(true) }
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

    // 快取宜忌列表
    val dayYi = remember(lunar) { lunar.dayYi }
    val dayJi = remember(lunar) { lunar.dayJi }

    // 生肖年表
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val zodiacList = remember(currentYear) {
        (-6..6).map { i ->
            val year = currentYear + i
            val l = Lunar.fromYmd(year, 6, 1)
            year to l.yearShengXiao
        }
    }

    // 狀態控制
    var showTermDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAuspiciousFullScreen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "黃曆",
                onSettingsClick = { showSettingsDialog = true },
                containerColor = MaterialTheme.colorScheme.surface
            )
        },
        containerColor = Color.Transparent // 讓 MainScreen 的淺灰色背景透過來
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 農曆大卡片 ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${lunar.monthInChinese}月 ${lunar.yearInGanZhi}年",
                        fontSize = 23.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = lunar.dayInChinese,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "【屬${lunar.yearShengXiao}】",
                        fontSize = 20.sp,
                        color = Color.Yellow,
                        textAlign = TextAlign.Center
                    )

                    // --- 新曆日期與星期 ---
                    Text(
                        text = SimpleDateFormat(
                            "yyyy年MM月dd日 EEEE",
                            Locale.TRADITIONAL_CHINESE
                        ).format(today),
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // --- 宜忌卡片 ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                YiJiRow(title = "宜", items = dayYi, color = Color(0xFF2E7D32))
                YiJiRow(title = "忌", items = dayJi, color = Color(0xFFC62828))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- 節氣詳情 ---
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp, // 增加陰影
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { showTermDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE0F2F1), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Eco,
                            contentDescription = null,
                            tint = Color(0xFF00695C),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentTermName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(color = Color(0xFF5D4037), shape = RoundedCornerShape(13.dp)) {
                                Text(
                                    text = "第${daysSince}天",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }

                        Text(text = "下一個節氣：$nextTermName")
                    }
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 生肖年表 ---
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val screenWidth = maxWidth
                val itemWidth = 75.dp
                val density = LocalDensity.current
                val zodiacListState = rememberLazyListState()
                LaunchedEffect(key1 = currentYear) {
                    zodiacListState.scrollToItem(
                        index = 6,
                        scrollOffset = -with(density) {
                            ((screenWidth - itemWidth) / 2).toPx().toInt()
                        })
                }
                LazyRow(
                    state = zodiacListState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(zodiacList) { _, (year, shengXiao) ->
                        ZodiacCard(
                            year = year,
                            shengXiao = shengXiao,
                            isCurrent = year == currentYear
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 吉日按鈕 ---
            ElevatedButton(
                onClick = { showAuspiciousFullScreen = true },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(54.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("查看吉日 (Auspicious Days)", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }


            // --- 對話框內容 ---
            if (showTermDialog) {
                val structuredData = remember(currentTermName) { SolarTermData.getStructuredData(currentTermName) }

                Dialog(
                    onDismissRequest = { showTermDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "節氣詳解",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                IconButton(onClick = { showTermDialog = false }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "關閉",
                                        tint = Color.LightGray
                                    )
                                }
                            }

                            Text(
                                text = currentTermName,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB71C1C)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (structuredData.isEmpty()) {
                                Text(
                                    text = SolarTermData.getDescription(currentTermName),
                                    fontSize = 16.sp,
                                    lineHeight = 28.sp,
                                    color = Color(0xFF5D4037)
                                )
                            } else {
                                structuredData.forEach { (title, content) ->
                                    SolarTermSection(title = title, content = content)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { showTermDialog = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text("我知道了", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (showSettingsDialog) AppSettingsDialog(onDismiss = { showSettingsDialog = false })

            // --- 全螢幕吉日查詢 ---
            if (showAuspiciousFullScreen) {
                Dialog(
                    onDismissRequest = { showAuspiciousFullScreen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false) // 關鍵：允許全螢幕
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 頂部導航列：整合標題與關閉按鈕
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF5D4037),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "吉日查詢",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                IconButton(onClick = { showAuspiciousFullScreen = false }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "關閉",
                                        tint = Color.Gray
                                    )
                                }
                            }
                            // 嵌入原本的吉日查詢頁面
                            AuspiciousQueryScreen()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YiJiRow(title: String, items: List<String>, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(color, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 4
        ) {
            val displayItems = items.take(8)
            if (displayItems.isEmpty()) {
                Text(text = "無", color = Color.Gray, fontSize = 16.sp)
            } else {
                displayItems.forEach { item ->
                    Text(
                        text = item,
                        fontSize = 20.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SolarTermSection(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(4.dp, 18.dp)
                    .background(Color(0xFFB71C1C), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB71C1C)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            fontSize = 16.sp,
            lineHeight = 26.sp,
            color = Color(0xFF5D4037)
        )
    }
}

@Composable
fun ZodiacCard(year: Int, shengXiao: String, isCurrent: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFFB71C1C) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isCurrent) 4.dp else 1.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .width(75.dp)
            .then(
                if (!isCurrent) Modifier.border(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(8.dp)
                ) else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = year.toString(),
                fontSize = 14.sp,
                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = shengXiao,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
