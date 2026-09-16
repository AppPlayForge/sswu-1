package com.example.myTools.birthday

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myTools.ui.RedBadgeNumber
import com.example.myTools.ui.ThreeDIconButton
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.ceil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BirthdayCard(
    record: BirthdayRecord,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) { configuration.locales[0] }

    var menuExpanded by remember { mutableStateOf(false) }

    val nextCal = remember(record.lunarMonth, record.lunarDay) {
        getNextBirthdayCalendar(record.lunarMonth, record.lunarDay)
    }
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val diffMillis = nextCal.timeInMillis - today.timeInMillis
    val daysLeft = ceil(diffMillis / (1000.0 * 60 * 60 * 24)).toInt()

    val solarDateStr = remember(nextCal, locale) {
        SimpleDateFormat("yyyy年MM月dd日", locale).format(nextCal.time)
    }
    val weekStr = remember(nextCal) {
        val days = arrayOf("日", "一", "二", "三", "四", "五", "六")
        val dayOfWeek = nextCal[Calendar.DAY_OF_WEEK]
        "星期${days[dayOfWeek - 1]}"
    }
    val timeStr = remember(record.remindHour, record.remindMinute, locale) {
        String.format(locale, "%02d:%02d", record.remindHour, record.remindMinute)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.combinedClickable(
            onClick = { /* 預留查看詳情 */ },
            onLongClick = onEdit,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 立體效果的主圖標與數字紅點
            Box {
                Surface(
                    shape = CircleShape,
                    color = if (daysLeft in 0..3) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Cake,
                            null,
                            tint = if (daysLeft in 0..3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                if (daysLeft in 0..3) {
                    RedBadgeNumber(
                        count = daysLeft,
                        showZero = true,
                        minSize = 18.dp,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "農曆： ${getLunarMonthName(record.lunarMonth)}${getLunarDayName(record.lunarDay)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "陽曆： $solarDateStr $weekStr",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (record.remindList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Alarm,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            " $timeStr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        record.remindList.sorted().forEach { days ->
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = if (days == 0) "當天" else "${days}天前",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (daysLeft == 0) {
                    Text("今天!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                } else if (daysLeft in 1..3) {
                    Text("還有 $daysLeft 天", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                } else {
                    Text("還有 $daysLeft 天", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }

                if (daysLeft in 0..3) {
                    Spacer(modifier = Modifier.width(8.dp))
                    RedBadgeNumber(
                        count = daysLeft,
                        showZero = true,
                        minSize = 18.dp,
                        fontSize = 11.sp
                    )
                }
            }

            Box {
                ThreeDIconButton(
                    icon = Icons.Default.MoreHoriz,
                    onClick = { menuExpanded = true }
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("複製") },
                        onClick = {
                            menuExpanded = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val copyText = "${record.name}的農曆生日：${getLunarMonthName(record.lunarMonth)}${getLunarDayName(record.lunarDay)}（陽曆：$solarDateStr $weekStr）"
                            val clip = ClipData.newPlainText("Birthday Info", copyText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "已複製生日資訊", Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "複製"
                            )
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("編輯") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "編輯"
                            )
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("刪除", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDeleteRequest()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "刪除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}
