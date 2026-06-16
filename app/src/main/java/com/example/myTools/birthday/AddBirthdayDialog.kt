package com.example.myTools.birthday

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBirthdayDialog(
    initialRecord: BirthdayRecord? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, List<Int>, List<Int>) -> Unit
) {
    var name by remember { mutableStateOf(initialRecord?.name ?: "") }
    var selectedMonth by remember { mutableIntStateOf(initialRecord?.lunarMonth ?: 1) }
    var selectedDay by remember { mutableIntStateOf(initialRecord?.lunarDay ?: 1) }
    var monthExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }

    val selectedRemindDays = remember {
        mutableStateListOf<Int>().apply { addAll(initialRecord?.remindList ?: listOf(1)) }
    }
    val selectedRemindHours = remember {
        mutableStateListOf<Int>().apply { addAll(initialRecord?.remindHours ?: listOf(9)) }
    }

    var errorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f),
        title = { Text(if (initialRecord == null) "新增農曆生日" else "修改農曆生日") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = monthExpanded,
                        onExpandedChange = { monthExpanded = !monthExpanded },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        OutlinedTextField(
                            value = getLunarMonthName(selectedMonth),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("月份") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                            modifier = Modifier.menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }) {
                            (1..12).forEach { m ->
                                DropdownMenuItem(text = {
                                    Text(
                                        getLunarMonthName(
                                            m
                                        )
                                    )
                                }, onClick = { selectedMonth = m; monthExpanded = false })
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = dayExpanded,
                        onExpandedChange = { dayExpanded = !dayExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = getLunarDayName(selectedDay),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("日期") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                            modifier = Modifier.menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false }) {
                            (1..30).forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(getLunarDayName(d)) },
                                    onClick = { selectedDay = d; dayExpanded = false })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("提醒時間 (可多選)：", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                val timeOptions = remember { listOf(9 to "上午9點", 14 to "下午2點", 19 to "晚上7點") }
                Row(modifier = Modifier.fillMaxWidth()) {
                    timeOptions.forEach { (hour, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (selectedRemindHours.contains(hour)) selectedRemindHours.remove(
                                        hour
                                    ) else selectedRemindHours.add(hour)
                                }) {
                            Checkbox(
                                checked = selectedRemindHours.contains(hour),
                                onCheckedChange = {
                                    if (it) selectedRemindHours.add(hour) else selectedRemindHours.remove(
                                        hour
                                    )
                                })
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("提醒日期 (可多選)：", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                val dayOptions = remember { listOf(0 to "當天", 1 to "1天前", 3 to "3天前", 7 to "7天前") }
                Column {
                    dayOptions.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { (days, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (selectedRemindDays.contains(days)) selectedRemindDays.remove(
                                                days
                                            ) else selectedRemindDays.add(days)
                                        }) {
                                    Checkbox(
                                        checked = selectedRemindDays.contains(days),
                                        onCheckedChange = {
                                            if (it) selectedRemindDays.add(days) else selectedRemindDays.remove(
                                                days
                                            )
                                        })
                                    Text(label, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                if (errorText.isNotEmpty()) Text(
                    text = errorText,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) errorText = "請輸入姓名" else onConfirm(
                    name,
                    selectedMonth,
                    selectedDay,
                    selectedRemindDays.toList(),
                    selectedRemindHours.toList()
                )
            }) { Text(if (initialRecord == null) "確定" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
