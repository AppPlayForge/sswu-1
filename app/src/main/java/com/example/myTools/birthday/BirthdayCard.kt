package com.example.myTools.birthday

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myTools.ui.ThreeDIconButton
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BirthdayCard(
    record: BirthdayRecord,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit
) {
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
    
    val solarDateStr = remember(nextCal) {
        SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(nextCal.time)
    }
    val weekStr = remember(nextCal) {
        val days = arrayOf("日", "一", "二", "三", "四", "五", "六")
        val dayOfWeek = nextCal.get(Calendar.DAY_OF_WEEK)
        "星期${days[dayOfWeek - 1]}"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.combinedClickable(
            onClick = { /* 預留查看詳情 */ },
            onLongClick = onEdit
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 立體效果的主圖標
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Cake,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "農曆： ${getLunarMonthName(record.lunarMonth)}${getLunarDayName(record.lunarDay)}",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "陽曆： $solarDateStr $weekStr",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.remindList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Alarm,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        val timeStr = record.remindHours.sorted().joinToString(", ") { h ->
                            when (h) {
                                9 -> "上午9點"; 14 -> "下午2點"; 19 -> "晚上7點"; else -> "$h:00"
                            }
                        }
                        Text(
                            " $timeStr",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        record.remindList.sorted().forEach { days ->
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = if (days == 0) "當天" else "${days}天前",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (daysLeft == 0) {
                Text("今天!", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            } else {
                Text("還有$daysLeft 天", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                ThreeDIconButton(icon = Icons.Default.Edit, onClick = onEdit)
                Spacer(modifier = Modifier.width(16.dp))
                ThreeDIconButton(icon = Icons.Default.Delete, onClick = onDeleteRequest)
            }
        }
    }
}
