package com.example.myTools.auspicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nlf.calendar.Lunar
import java.util.Calendar

//吉日查詢
@Composable
fun AuspiciousQueryScreen() {
    // --- 狀態管理 ---
    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var selectedCategory by remember { mutableStateOf("嫁娶") }

    // 控制年月選擇對話框的開關
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // 當前點選查看詳情的吉日
    var selectedDayDetail by remember { mutableStateOf<Lunar?>(null) }

    // 搜尋結果列表
    val auspiciousDays = remember(selectedYear, selectedMonth, selectedCategory) {
        findAuspiciousDays(selectedYear, selectedMonth, selectedCategory)
    }

    val categories = listOf(
        "嫁娶", "開市", "入宅", "移徙", "動土",
        "出行", "祈福", "祭祀", "安床", "裁衣",
        "理髮", "納財", "交易", "安葬", "求醫",
        "治病", "拆卸", "修造", "上樑", "破土"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
    ) {
        // 2. 年月選擇器
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp), // 稍微縮小內邊距
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 上個月
                IconButton(onClick = {
                    if (selectedMonth == 1) { selectedMonth = 12; selectedYear-- } else { selectedMonth-- }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Gray)
                }

                // 點擊文字彈出對話框
                Row(
                    modifier = Modifier
                        .clickable { showDatePickerDialog = true } // 點擊觸發
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFFB71C1C), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$selectedYear 年 $selectedMonth 月",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C)
                    )
                }

                // 下個月 (微調)
                IconButton(onClick = {
                    if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ } else { selectedMonth++ }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 類別選擇 (LazyHorizontalGrid 雙排顯示)
        Text("選擇事項：", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

        LazyHorizontalGrid(
            rows = GridCells.Fixed(2), // 固定 2 行
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp), // 給定一個固定高度，大約足夠放兩排 Chip
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFB71C1C),
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 結果列表
        Text(
            text = "本月共有 ${auspiciousDays.size} 個吉日",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(auspiciousDays) { lunar ->
                AuspiciousDayCard(lunar, onClick = { selectedDayDetail = lunar })
            }
        }
    }

    // ★ 彈出詳情對話框 ★
    selectedDayDetail?.let { lunar ->
        AuspiciousDetailDialog(
            lunar = lunar,
            onDismiss = { selectedDayDetail = null }
        )
    }

    // ★ 彈出年月選擇對話框 ★
    if (showDatePickerDialog) {
        YearMonthPickerDialog(
            initialYear = selectedYear,
            initialMonth = selectedMonth,
            onDismiss = { showDatePickerDialog = false },
            onConfirm = { year, month ->
                selectedYear = year
                selectedMonth = month
                showDatePickerDialog = false
            }
        )
    }
}

// --- 自定義的年月選擇對話框 ---
@Composable
fun YearMonthPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    // 臨時狀態，只在對話框內部變化
    var tempYear by remember { mutableIntStateOf(initialYear) }
    // 注意：這裡 month 用 1-12
    var tempMonth by remember { mutableIntStateOf(initialMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("選擇日期") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 1. 年份選擇器 (左右箭頭 + 大字)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { tempYear-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "上一年")
                    }
                    Text(
                        text = "$tempYear 年",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(onClick = { tempYear++ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "下一年")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // 2. 月份選擇器 (3列網格，直接點選)
                Text("選擇月份", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4), // 4列，這樣 12 個月剛好 3 行
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items((1..12).toList()) { month ->
                        val isSelected = month == tempMonth
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFB71C1C) else Color(0xFFEEEEEE))
                                .clickable { tempMonth = month }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${month}月",
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tempYear, tempMonth) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
            ) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.Gray)
            }
        }
    )
}

// --- 吉日詳情對話框 ---
@Composable
fun AuspiciousDetailDialog(
    lunar: Lunar,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "吉日詳情",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 公曆農曆對比
                DetailRow("公曆", "${lunar.solar.year}年${lunar.solar.month}月${lunar.solar.day}日 ${lunar.weekInChinese}")
                DetailRow("農曆", "${lunar.yearInChinese}年${lunar.monthInChinese}月${lunar.dayInChinese}")
                DetailRow("干支", "${lunar.yearInGanZhi}年 ${lunar.monthInGanZhi}月 ${lunar.dayInGanZhi}日")

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 宜
                Row {
                    Text("【宜】", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = lunar.dayYi.joinToString("、"),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                // 忌
                Row {
                    Text("【忌】", color = Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = lunar.dayJi.joinToString("、"),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 沖煞
                Text(
                    text = "沖：${lunar.dayChongDesc} | 煞：${lunar.daySha}",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("確定", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row {
        Text("$label：", color = Color.Gray, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        Text(value, color = Color.Black, fontSize = 16.sp)
    }
}

// 卡片組件
@Composable
fun AuspiciousDayCard(lunar: Lunar, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側日期區域優化 (添加農曆標識與圖標感)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(60.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color(0xFFB71C1C),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = lunar.dayInChinese, // 使用 "十六" 而非 "16"，更有農曆感
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB71C1C)
                )
                Text(
                    text = "周${lunar.weekInChinese}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "陽曆：${lunar.solar.year}年${lunar.solar.month}月${lunar.solar.day}日",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${lunar.monthInGanZhi}月 ${lunar.dayInGanZhi}日",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "沖${lunar.dayChongDesc} ${lunar.daySha}",
                    fontSize = 16.sp,
                    color = Color.Red.copy(alpha = 0.7f)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x222E7D32), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text("宜", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- 邏輯：計算吉日 (使用 Calendar 修復版) ---
fun findAuspiciousDays(year: Int, month: Int, category: String): List<Lunar> {
    val results = mutableListOf<Lunar>()

    // 定義同義詞與繁簡對照表
    // Key: UI 顯示的名稱 (繁體)
    // Value: 庫可能返回的所有可能性 (包含簡體、古稱)
    val categoryMap = mapOf(
        "理髮" to listOf("理髮", "剃頭", "理发", "剃头", "整手足甲"),
        "求醫" to listOf("求醫", "求醫療病", "療病", "求医", "求医疗病", "疗病"),
        "開市" to listOf("開市", "開業", "開工", "开市", "开业", "开工"),
        "動土" to listOf("動土", "动土"),
        "納財" to listOf("納財", "置產", "纳财", "置产"),
        "交易" to listOf("交易", "立券", "立券交易"),
        "祈福" to listOf("祈福", "酬神"),
        "出行" to listOf("出行", "出遊"),
        "修造" to listOf("修造", "裝修"),
        "移徙" to listOf("移徙", "搬家", "喬遷"),
        "上樑" to listOf("上梁")
    )

    // 獲取所有可能的關鍵字 (如果 Map 裡沒有，就用原本的)
    val searchKeywords = categoryMap[category] ?: listOf(category)

    val calendar = Calendar.getInstance()
    calendar.set(year, month - 1, 1)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    for (day in 1..daysInMonth) {
        calendar.set(year, month - 1, day)
        val lunar = Lunar.fromDate(calendar.time)

        // ★ 核心搜尋邏輯：檢查當天的"宜"列表，是否包含我們關鍵字列表中的"任何一個"
        val dayYiList = lunar.dayYi // 這是庫返回的列表 (可能是簡體，也可能是古文)

        // 判斷 dayYiList 裡面有沒有任何一個詞，存在於我們的 searchKeywords 裡
        val isMatch = dayYiList.any { yiItem ->
            searchKeywords.contains(yiItem)
        }

        if (isMatch) {
            results.add(lunar)
        }
    }
    return results
}