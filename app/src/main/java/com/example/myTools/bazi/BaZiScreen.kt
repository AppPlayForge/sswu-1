package com.example.myTools.bazi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myTools.settings.AppSettingsDialog
import com.example.myTools.ui.CommonTopBar
import com.example.myTools.ui.DeleteConfirmDialog
import com.nlf.calendar.EightChar
import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import java.util.Locale

@Composable
fun BaZiScreen() {
    val context = LocalContext.current
    var records by remember { mutableStateOf(BaZiManager.loadList(context)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<BaZiRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<BaZiRecord?>(null) }
    var selectedRecord by remember { mutableStateOf<BaZiRecord?>(null) }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "八字命盤",
                onSettingsClick = { showSettingsDialog = true }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加紀錄")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暫無紀錄，請點擊右下角按鈕添加", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(records) { record ->
                        BaZiRecordItem(
                            record = record,
                            onClick = { selectedRecord = record },
                            onEdit = { recordToEdit = record },
                            onDelete = { recordToDelete = record }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBaZiDialog(
            onDismiss = { showAddDialog = false },
            onSave = {
                BaZiManager.addOrUpdateRecord(context, it)
                records = BaZiManager.loadList(context)
                showAddDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        AppSettingsDialog(onDismiss = { showSettingsDialog = false })
    }

    if (recordToEdit != null) {
        AddBaZiDialog(
            initialRecord = recordToEdit,
            onDismiss = { recordToEdit = null },
            onSave = {
                BaZiManager.addOrUpdateRecord(context, it)
                records = BaZiManager.loadList(context)
                recordToEdit = null
            }
        )
    }

    if (recordToDelete != null) {
        DeleteConfirmDialog(
            message = "確定要刪除 ${recordToDelete!!.name} 的八字紀錄嗎？",
            onDismiss = { recordToDelete = null },
            onConfirm = {
                BaZiManager.deleteRecord(context, recordToDelete!!.id)
                records = BaZiManager.loadList(context)
                recordToDelete = null
            }
        )
    }

    if (selectedRecord != null) {
        BaZiDetailDialog(
            record = selectedRecord!!,
            onDismiss = { selectedRecord = null }
        )
    }
}

@Composable
fun BaZiRecordItem(
    record: BaZiRecord,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 立體效果的主圖標
            Surface(
                shape = CircleShape,
                color = Color(0xFFF5F5F5),
                shadowElevation = 4.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint = Color(0xFF616161),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                val typeStr = if (record.isLunar) "農曆" else "公曆"
                Text(
                    text = "$typeStr: ${record.year}-${record.month}-${record.day} ${
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            record.hour,
                            record.minute
                        )
                    }",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                ThreeDIconButton(icon = Icons.Default.Edit, onClick = onEdit)
                Spacer(modifier = Modifier.width(12.dp))
                ThreeDIconButton(icon = Icons.Default.Delete, onClick = onDelete)
            }
        }
    }
}

@Composable
fun ThreeDIconButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFFF5F5F5),
        shadowElevation = 3.dp,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF616161),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaZiDetailDialog(
    record: BaZiRecord,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lunar = if (record.isLunar) {
        Lunar.fromYmdHms(record.year, record.month, record.day, record.hour, record.minute, 0)
    } else {
        Solar.fromYmdHms(record.year, record.month, record.day, record.hour, record.minute, 0).lunar
    }

    val solar = lunar.solar
    val solarFullStr = "${solar.year}-${
        String.format(
            Locale.getDefault(),
            "%02d-%02d %02d:%02d",
            solar.month,
            solar.day,
            solar.hour,
            solar.minute
        )
    }"
    val baZi = lunar.eightChar

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("${record.name} 的八字命盤") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "關閉")
                            }
                        },
                        actions = {
                            // 複製按鈕放在顯眼位置
                            TextButton(onClick = {
                                val copyText = """
                                   你是一位精通中國傳統命理學的玄學大家，
                                   融合了「子平八字」、 「紫微斗數」與「奇門遁甲」三家之長。
                                   現在請為以下緣主進行深度、詳細的「綜合命書」論斷。
                                   
                                   【緣主基本資料】
                                    姓名：${record.name}
                                    出生公曆：${solar.toFullString()}
                                    出生農曆：$lunar
                                    
                                    八字四柱：
                                    年柱：${baZi.year} (${baZi.yearShiShenGan}, 納音: ${baZi.yearNaYin})
                                    月柱：${baZi.month} (${baZi.monthShiShenGan}, 納音: ${baZi.monthNaYin})
                                    日柱：${baZi.day} (日主, 納音: ${baZi.dayNaYin})
                                    時柱：${baZi.time} (${baZi.timeShiShenGan}, 納音: ${baZi.timeWuXing})
                                    
                                    五行分布：${baZi.yearWuXing}${baZi.monthWuXing}${baZi.dayWuXing}${baZi.timeWuXing}
                                    
                                    請依據上述資料，撰寫一份結構嚴謹、條理分明且極具深度與細節的「個人命書」，並嚴格依據以下五大核心模塊展開詳細分析：
                                    ---
                                    ### 一、 命格總攬與性格特質深度剖析
                                    1. 【八字格局】分析日主強弱、定格（如食神格、正官格等），並說明此格局的核心心性。
                                    2. 【紫微主星】結合紫微斗數，分析命宮、身宮的主星與輔星組合（如紫微獨坐、殺破狼格等），論述其外在顯化與內在隱藏的性格。
                                    3. 【性格雙重性】請以中立客觀的角度，既指出其性格上的優勢與大氣之處，也要直言不諱地指出其潛在的性格盲點、心魔或容易自我糾結的地方。

                                    ### 二、 五行喜忌與轉運指南（行業、服飾、顏色）
                                    1. 【喜用神推算】精確找出本命的「喜神」與「用神」，並說明為何此五行能起到調候、通關或扶抑的作用。
                                    2. 【適合行業與發展方位】
                                       - 根據喜用五行，列出具體的現代職業與行業方向（請給出具體且符合現代社會的行業，而非古代術語）。
                                       - 指出最適合求財與發展的「地理方位」（以出生地為基準）。
                                    3. 【開運美學與生活日常】
                                       - 適合的服裝顏色、日常幸運色搭配建議。
                                       - 適合的材質（如金屬、木質、水晶等）或配飾建議。

                                    ### 三、 財運與事業格局（一生財富論）
                                    1. 【財庫與財源】分析八字中的正財、偏財透藏情況，以及紫微斗數中「財帛宮」與「田宅宮」的吉凶，論述此生是屬於「勞碌求財」、「穩健聚財」還是「暴發型財運」。
                                    2. 【奇門財利局】引入奇門遁甲視角，推算其一生財路中容易遇到的「生門」與「傷門」特徵，並給予具體的守財與投資理財建議。

                                    ### 四、 官非、小人與人生關煞（風險預警）
                                    1. 【官非與訴訟風險】分析八字中是否帶有「傷官見官」、「梟神奪食」或地支刑沖，以及紫微斗數中「官祿宮/遷移宮」是否有擎羊、陀羅、化忌等煞星引動。
                                    2. 【小人與口舌】評估一生中何時容易遭遇小人中傷、合夥背叛或合同糾紛（官非）。
                                    3. 【趨吉避凶指南】給出具體的心態調整與行事作風建議，如何防範法律風險與人際衝突。

                                    ### 五、 歲運流年與奇門開運總結
                                    1. 【大運起伏趨勢】簡述目前所行大運的吉凶交接點，未來幾年（特別是近3-5年）需要注意的關鍵年份。
                                    2. 【奇門局終極點評】以奇門遁甲的「三奇六儀」與「八門九星」為這份命書做一個總結性的開運寄語，指引人生方向。
                                    ---
                                    提示：請保持語氣專業、悲憫、客觀、直言不諱。多用具體的命理術語搭配白話文詳細解釋，避免流於表面、模稜兩可的套話。請開始你詳細的論斷。 
                                """.trimIndent()
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("BaZi Data", copyText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(
                                    context,
                                    "命盤數據已複製，請粘貼到 AI 進行諮詢",
                                    Toast.LENGTH_LONG
                                ).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("複製數據諮詢 AI")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 基本信息卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("公曆出生", solarFullStr)
                            InfoRow("農曆出生", lunar.toString())
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 八字核心展示
                    Text(
                        "八字四柱",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BaZiColumn(
                            "年柱",
                            baZi.year,
                            baZi.yearShiShenGan,
                            baZi.yearWuXing,
                            baZi.yearNaYin
                        )
                        BaZiColumn(
                            "月柱",
                            baZi.month,
                            baZi.monthShiShenGan,
                            baZi.monthWuXing,
                            baZi.monthNaYin
                        )
                        BaZiColumn("日柱", baZi.day, "日主", baZi.dayWuXing, baZi.dayNaYin)
                        BaZiColumn(
                            "時柱",
                            baZi.time,
                            baZi.timeShiShenGan,
                            baZi.timeWuXing,
                            baZi.timeNaYin
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 五行平衡
                    Text(
                        "五行平衡",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        calculateWuXingBalance(baZi),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 命理數據卡片
                    Text(
                        "命理詳情",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("胎元", baZi.taiYuan)
                            InfoRow("命宮", baZi.mingGong)
                            InfoRow("身宮", baZi.shenGong)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        "地支藏幹 (十神)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CangGanItem("年支藏幹", baZi.yearHideGan)
                        CangGanItem("月支藏幹", baZi.monthHideGan)
                        CangGanItem("日支藏幹", baZi.dayHideGan)
                        CangGanItem("時支藏幹", baZi.timeHideGan)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun CangGanItem(label: String, list: List<String>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = "$label: ", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text(text = list.joinToString("  "))
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ",
            fontSize = 20.sp
        )
        Text(text = value, fontSize = 18.sp)
    }
}

@Composable
fun BaZiColumn(label: String, value: String, tenShi: String, wuXing: String, naYin: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(tenShi, fontSize = 16.sp, color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(wuXing, fontSize = 16.sp, color = Color(0xFF1B5E20))
        Text(naYin, fontSize = 14.sp, color = Color.Gray)
    }
}

fun calculateWuXingBalance(baZi: EightChar): String {
    val all = baZi.yearWuXing + baZi.monthWuXing + baZi.dayWuXing + baZi.timeWuXing
    val counts = mutableMapOf('金' to 0, '木' to 0, '水' to 0, '火' to 0, '土' to 0)
    all.forEach { char ->
        if (counts.containsKey(char)) {
            counts[char] = counts[char]!! + 1
        }
    }
    return counts.entries.joinToString("  ") { "${it.key}: ${it.value}" }
}
