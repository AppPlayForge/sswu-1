package com.example.myTools.bazi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBaZiDialog(
    initialRecord: BaZiRecord? = null,
    onDismiss: () -> Unit,
    onSave: (BaZiRecord) -> Unit
) {
    // 改用 TextFieldValue 以便控制選取範圍 (Selection)
    var surname by remember { mutableStateOf(TextFieldValue(initialRecord?.surname ?: "")) }
    var givenName by remember { mutableStateOf(TextFieldValue(initialRecord?.givenName ?: "")) }
    var year by remember { mutableStateOf(TextFieldValue(initialRecord?.year?.toString() ?: "1990")) }
    var month by remember { mutableStateOf(TextFieldValue(initialRecord?.month?.toString() ?: "1")) }
    var day by remember { mutableStateOf(TextFieldValue(initialRecord?.day?.toString() ?: "1")) }
    var hour by remember { mutableStateOf(TextFieldValue(initialRecord?.hour?.toString() ?: "12")) }
    var minute by remember { mutableStateOf(TextFieldValue(initialRecord?.minute?.toString() ?: "0")) }
    var gender by remember { mutableStateOf(initialRecord?.gender ?: "男") }
    var province by remember { mutableStateOf(TextFieldValue(initialRecord?.province ?: "")) }
    var city by remember { mutableStateOf(TextFieldValue(initialRecord?.city ?: "")) }
    var isLunar by remember { mutableStateOf(initialRecord?.isLunar ?: false) }

    val focusManager = LocalFocusManager.current
    
    val focusRequesterSurname = remember { FocusRequester() }
    val focusRequesterGivenName = remember { FocusRequester() }
    val focusRequesterYear = remember { FocusRequester() }
    val focusRequesterMonth = remember { FocusRequester() }
    val focusRequesterDay = remember { FocusRequester() }
    val focusRequesterHour = remember { FocusRequester() }
    val focusRequesterMinute = remember { FocusRequester() }
    val focusRequesterProvince = remember { FocusRequester() }
    val focusRequesterCity = remember { FocusRequester() }

    // 驗證狀態 (讀取 .text 屬性)
    val yearInt = year.text.toIntOrNull()
    val monthInt = month.text.toIntOrNull()
    val dayInt = day.text.toIntOrNull()
    val hourInt = hour.text.toIntOrNull()
    val minuteInt = minute.text.toIntOrNull()

    val isYearError = yearInt == null || yearInt !in 1900..2100
    val isMonthError = monthInt == null || monthInt !in 1..12
    
    // 農曆最大 30 天，公曆最大 31 天
    val maxDay = if (isLunar) 30 else 31
    val isDayError = dayInt == null || dayInt !in 1..maxDay
    
    val isHourError = hourInt == null || hourInt !in 0..23
    val isMinuteError = minuteInt == null || minuteInt !in 0..59

    val inputTextStyle = TextStyle(fontSize = 18.sp)
    val labelTextStyle = TextStyle(fontSize = 14.sp)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(0.92f), // 控制對話框佔螢幕寬度的比例
        title = { 
            Text(
                if (initialRecord == null) "添加八字紀錄" else "編輯八字紀錄",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = surname,
                        onValueChange = { surname = it },
                        label = { Text("姓氏", style = labelTextStyle) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterSurname)
                            .onFocusChanged { if (it.isFocused) surname = surname.copy(selection = TextRange(0, surname.text.length)) },
                        singleLine = true,
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterGivenName.requestFocus() })
                    )
                    OutlinedTextField(
                        value = givenName,
                        onValueChange = { givenName = it },
                        label = { Text("名字", style = labelTextStyle) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterGivenName)
                            .onFocusChanged { if (it.isFocused) givenName = givenName.copy(selection = TextRange(0, givenName.text.length)) },
                        singleLine = true,
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterYear.requestFocus() })
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("性別", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                    Spacer(modifier = Modifier.width(16.dp))
                    listOf("男", "女").forEach { g ->
                        val isSelected = gender == g
                        Button(
                            onClick = { gender = g },
                            colors = if (isSelected) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(g)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("農曆模式", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = isLunar, onCheckedChange = { isLunar = it })
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Text("出生時間", style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("年", style = labelTextStyle) },
                        modifier = Modifier
                            .weight(1.2f)
                            .focusRequester(focusRequesterYear)
                            .onFocusChanged { if (it.isFocused) year = year.copy(selection = TextRange(0, year.text.length)) },
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterMonth.requestFocus() }),
                        isError = isYearError,
                        supportingText = {
                            if (isYearError) Text("1900-2100", color = MaterialTheme.colorScheme.error)
                        }
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it },
                        label = {
                            val label = if (isLunar) "月 (${getLunarMonthName(monthInt ?: 0)})" else "月"
                            Text(label, style = labelTextStyle)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .focusRequester(focusRequesterMonth)
                            .onFocusChanged { if (it.isFocused) month = month.copy(selection = TextRange(0, month.text.length)) },
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterDay.requestFocus() }),
                        isError = isMonthError,
                        supportingText = {
                            if (isMonthError) Text("1-12月", color = MaterialTheme.colorScheme.error)
                        }
                    )
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = {
                            val label = if (isLunar) "日 (${getLunarDayName(dayInt ?: 0)})" else "日"
                            Text(label, style = labelTextStyle)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .focusRequester(focusRequesterDay)
                            .onFocusChanged { if (it.isFocused) day = day.copy(selection = TextRange(0, day.text.length)) },
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterHour.requestFocus() }),
                        isError = isDayError,
                        supportingText = {
                            if (isDayError) Text("1-${maxDay}日", color = MaterialTheme.colorScheme.error)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("出生地點", style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it },
                        label = { 
                            Text("時 (${getHourBranchName(hourInt ?: 0)})", style = labelTextStyle) 
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterHour)
                            .onFocusChanged { if (it.isFocused) hour = hour.copy(selection = TextRange(0, hour.text.length)) },
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterMinute.requestFocus() }),
                        isError = isHourError,
                        supportingText = {
                            if (isHourError) Text("0-23", color = MaterialTheme.colorScheme.error)
                        }
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it },
                        label = {
                            Text("分", style = labelTextStyle)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterMinute)
                            .onFocusChanged { if (it.isFocused) minute = minute.copy(selection = TextRange(0, minute.text.length)) },
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterProvince.requestFocus() }),
                        isError = isMinuteError,
                        supportingText = {
                            if (isMinuteError) Text("0-59", color = MaterialTheme.colorScheme.error)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("出生地點", style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = province,
                        onValueChange = { province = it },
                        label = { Text("出生省份", style = labelTextStyle) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterProvince)
                            .onFocusChanged { if (it.isFocused) province = province.copy(selection = TextRange(0, province.text.length)) },
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterCity.requestFocus() })
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("城市", style = labelTextStyle) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterCity)
                            .onFocusChanged { if (it.isFocused) city = city.copy(selection = TextRange(0, city.text.length)) },
                        textStyle = inputTextStyle,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isYearError && !isMonthError && !isDayError && !isHourError && !isMinuteError,
                onClick = {
                    val record = BaZiRecord(
                        id = initialRecord?.id ?: System.currentTimeMillis(),
                        surname = surname.text,
                        givenName = givenName.text,
                        gender = gender,
                        year = yearInt!!,
                        month = monthInt!!,
                        day = dayInt!!,
                        hour = hourInt!!,
                        minute = minuteInt!!,
                        province = province.text,
                        city = city.text,
                        isLunar = isLunar
                    )
                    onSave(record)
                }
            ) {
                Text("保存", fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", fontSize = 16.sp) }
        }
    )
}

private fun getLunarMonthName(m: Int): String {
    val names = listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "臘")
    return if (m in 1..12) "${names[m - 1]}月" else "${m}月"
}

private fun getLunarDayName(d: Int): String {
    val first = listOf("初", "十", "廿", "卅")
    val second = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
    return when {
        d == 10 -> "初十"
        d == 20 -> "二十"
        d == 30 -> "三十"
        d in 1..30 -> "${first[(d - 1) / 10]}${second[(d - 1) % 10]}"
        else -> d.toString()
    }
}

private fun getHourBranchName(h: Int): String {
    return when (h) {
        23, 0 -> "子時"
        1, 2 -> "丑時"
        3, 4 -> "寅時"
        5, 6 -> "卯時"
        7, 8 -> "辰時"
        9, 10 -> "巳時"
        11, 12 -> "午時"
        13, 14 -> "未時"
        15, 16 -> "申時"
        17, 18 -> "酉時"
        19, 20 -> "戌時"
        21, 22 -> "亥時"
        else -> "${h}點"
    }
}
