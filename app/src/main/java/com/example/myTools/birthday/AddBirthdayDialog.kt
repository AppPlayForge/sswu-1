package com.example.myTools.birthday

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBirthdayDialog(
    initialRecord: BirthdayRecord? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, List<Int>, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialRecord?.name ?: "") }
    var selectedMonth by remember { mutableIntStateOf(initialRecord?.lunarMonth ?: 1) }
    var selectedDay by remember { mutableIntStateOf(initialRecord?.lunarDay ?: 1) }
    var monthExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }

    val selectedRemindDays = remember {
        mutableStateListOf<Int>().apply { 
            if (initialRecord != null) {
                addAll(initialRecord.remindList)
            } else {
                add(0) // 默認當天
            }
        }
    }
    
    var remindHour by remember { mutableIntStateOf(initialRecord?.remindHour ?: 9) }
    var remindMinute by remember { mutableIntStateOf(initialRecord?.remindMinute ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }

    var errorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f),
        title = { Text(if (initialRecord == null) "新增農曆生日" else "修改農曆生日") },
        text = {
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("稱呼") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    })
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
                                    Text(getLunarMonthName(m))
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
                
                Text("提醒時間：", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                OutlinedCard(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", remindHour, remindMinute),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("(點擊修改)", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("提醒日期 (可多選)：", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                val dayOptions = remember { listOf(0 to "當天", 1 to "1天前", 2 to "2天前", 3 to "3天前") }
                Column {
                    dayOptions.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { (days, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (selectedRemindDays.contains(days)) {
                                                if (selectedRemindDays.size > 1) selectedRemindDays.remove(days)
                                            } else {
                                                selectedRemindDays.add(days)
                                            }
                                        }) {
                                    Checkbox(
                                        checked = selectedRemindDays.contains(days),
                                        onCheckedChange = {
                                            if (it) {
                                                selectedRemindDays.add(days)
                                            } else {
                                                if (selectedRemindDays.size > 1) selectedRemindDays.remove(days)
                                            }
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
                if (name.isBlank()) errorText = "請輸入稱呼"
                else if (selectedRemindDays.isEmpty()) errorText = "請至少選擇一個提醒日期"
                else onConfirm(
                    name,
                    selectedMonth,
                    selectedDay,
                    selectedRemindDays.toList(),
                    remindHour,
                    remindMinute
                )
            }) { Text(if (initialRecord == null) "確定" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = remindHour,
            initialMinute = remindMinute
        )
        
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "選擇提醒時間",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("取消") }
                        TextButton(onClick = {
                            remindHour = timePickerState.hour
                            remindMinute = timePickerState.minute
                            showTimePicker = false
                        }) { Text("確定") }
                    }
                }
            }
        }
    }
}
