package com.example.myTools.note

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myTools.ui.TagChip

@Composable
fun NoteEditDialog(
    note: NoteRecord,
    onDismiss: () -> Unit,
    onSave: (NoteRecord) -> Unit,
    onSaveAsTxt: (NoteRecord) -> Unit,
    onDelete: (NoteRecord) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var title by remember { mutableStateOf(note.title) }
    var contentValue by remember {
        mutableStateOf(TextFieldValue(text = note.content, selection = TextRange(note.content.length)))
    }
    var isPinned by remember { mutableStateOf(note.isPinned) }
    var selectedColorHex by remember { mutableStateOf(note.colorHex ?: "") }
    var isChecklist by remember { mutableStateOf(note.isChecklist) }
    var checklistItems by remember { mutableStateOf(note.checklistItems) }
    var isCompletedExpanded by remember { mutableStateOf(true) }
    var focusTargetItemId by remember { mutableStateOf<String?>(null) }
    val contentFocusRequester = remember { FocusRequester() }

    fun addNewChecklistItemAfter(currentItem: ChecklistItem? = null) {
        val newItem = ChecklistItem()
        if (currentItem != null) {
            val currentIndex = checklistItems.indexOfFirst { it.id == currentItem.id }
            checklistItems = if (currentIndex != -1) {
                checklistItems.toMutableList().apply { add(currentIndex + 1, newItem) }
            } else {
                checklistItems + newItem
            }
        } else {
            checklistItems = checklistItems + newItem
        }
        focusTargetItemId = newItem.id
    }

    val colorOpt = NOTE_COLORS.find { it.hex == selectedColorHex }
    val dialogBg = when {
        colorOpt != null && colorOpt.hex.isNotEmpty() -> if (isDark) colorOpt.darkColor else colorOpt.lightColor
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val detectedTags = remember(title, contentValue.text, isChecklist, checklistItems) {
        val fullText = if (isChecklist) {
            "$title ${checklistItems.joinToString(" ") { it.text }} ${contentValue.text}"
        } else {
            "$title ${contentValue.text}"
        }
        NoteRecord.extractHashtags(fullText)
    }

    fun buildCurrentNote(): NoteRecord {
        return note.copy(
            title = title,
            content = contentValue.text,
            isPinned = isPinned,
            colorHex = selectedColorHex.ifEmpty { null },
            isChecklist = isChecklist,
            checklistItems = checklistItems
        )
    }

    fun hasContent(): Boolean {
        return title.isNotBlank() || contentValue.text.isNotBlank() || checklistItems.any { it.text.isNotBlank() }
    }

    fun toggleChecklistMode() {
        if (!isChecklist) {
            // 切換至清單模式：將原本 content 的各行轉為清單項目
            if (checklistItems.isEmpty()) {
                val lines = contentValue.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isNotEmpty()) {
                    checklistItems = lines.map { ChecklistItem(text = it) }
                } else {
                    val firstItem = ChecklistItem()
                    checklistItems = listOf(firstItem)
                    focusTargetItemId = firstItem.id
                }
            } else {
                focusTargetItemId = checklistItems.firstOrNull()?.id
            }
            isChecklist = true
        } else {
            // 切換回文字模式：將清單項目合併為 content
            if (contentValue.text.isBlank() && checklistItems.isNotEmpty()) {
                val newText = checklistItems.joinToString("\n") { it.text }
                contentValue = TextFieldValue(text = newText, selection = TextRange(newText.length))
            }
            isChecklist = false
        }
    }

    fun insertTagSymbol() {
        if (isChecklist) {
            val newItem = ChecklistItem(text = "#")
            checklistItems = checklistItems + newItem
            focusTargetItemId = newItem.id
        } else {
            val currentText = contentValue.text
            val selection = contentValue.selection
            val cursor = selection.start.coerceIn(0, currentText.length)

            val before = currentText.substring(0, cursor)
            val after = currentText.substring(cursor)

            val prefix = if (before.isNotEmpty() && !before.endsWith(" ") && !before.endsWith("\n")) " #" else "#"
            val newText = before + prefix + after
            val newCursor = before.length + prefix.length

            contentValue = TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
            try {
                contentFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = {
            if (hasContent()) {
                onSave(buildCurrentNote())
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = dialogBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // 頂部導航列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (hasContent()) {
                            onSave(buildCurrentNote())
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "關閉")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 切換 清單/文字 模式按鈕
                        IconButton(onClick = { toggleChecklistMode() }) {
                            Icon(
                                imageVector = if (isChecklist) Icons.AutoMirrored.Filled.FormatListBulleted else Icons.Default.CheckBox,
                                contentDescription = "切換清單模式",
                                tint = if (isChecklist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 插入 #標籤 按鈕 (移至頂部)
                        IconButton(onClick = { insertTagSymbol() }) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = "插入 #標籤",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (note.id != 0L) {
                            IconButton(onClick = { onDelete(note) }) {
                                Icon(Icons.Default.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Button(
                            onClick = {
                                onSave(buildCurrentNote())
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("保存")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 標題輸入框
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("標題", style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.outline)) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // 內容區域 (單純文字 或 清單項目)
                Box(modifier = Modifier.weight(1f)) {
                    if (!isChecklist) {
                        TextField(
                            value = contentValue,
                            onValueChange = { contentValue = it },
                            placeholder = { Text("記事文本 (點擊頂部 # 可快速新增標籤)...", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.outline)) },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(contentFocusRequester)
                        )
                    } else {
                        val uncheckedItems = checklistItems.filter { !it.isChecked }
                        val checkedItems = checklistItems.filter { it.isChecked }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // 1. 未勾選清單項目
                            uncheckedItems.forEach { item ->
                                val focusRequester = remember { FocusRequester() }

                                LaunchedEffect(focusTargetItemId) {
                                    if (focusTargetItemId == item.id) {
                                        try {
                                            focusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                        focusTargetItemId = null
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "拖曳排序",
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            checklistItems = checklistItems.map {
                                                if (it.id == item.id) it.copy(isChecked = true) else it
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckBoxOutlineBlank,
                                            contentDescription = "勾選",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    TextField(
                                        value = item.text,
                                        onValueChange = { newText ->
                                            checklistItems = checklistItems.map {
                                                if (it.id == item.id) it.copy(text = newText) else it
                                            }
                                        },
                                        placeholder = { Text("清單項目", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.outline)) },
                                        textStyle = MaterialTheme.typography.bodyLarge,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(
                                            onNext = { addNewChecklistItemAfter(item) },
                                            onDone = { addNewChecklistItemAfter(item) }
                                        ),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusRequester(focusRequester)
                                    )

                                    IconButton(
                                        onClick = {
                                            checklistItems = checklistItems.filter { it.id != item.id }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "刪除項目",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // 2. 新增清單項目按鈕
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        addNewChecklistItemAfter(null)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "清單項目",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            // 3. 已勾選的項目 (展開/折疊區塊)
                            if (checkedItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { isCompletedExpanded = !isCompletedExpanded }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCompletedExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${checkedItems.size} 個勾選的項目",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                AnimatedVisibility(visible = isCompletedExpanded) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        checkedItems.forEach { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Spacer(modifier = Modifier.width(20.dp)) // 保持縮排

                                                IconButton(
                                                    onClick = {
                                                        checklistItems = checklistItems.map {
                                                            if (it.id == item.id) it.copy(isChecked = false) else it
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckBox,
                                                        contentDescription = "取消勾選",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                Text(
                                                    text = item.text,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        textDecoration = TextDecoration.LineThrough
                                                    ),
                                                    color = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(horizontal = 4.dp)
                                                )

                                                IconButton(
                                                    onClick = {
                                                        checklistItems = checklistItems.filter { it.id != item.id }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "刪除項目",
                                                        tint = MaterialTheme.colorScheme.outline,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (detectedTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "已識別標籤：",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            detectedTags.forEach { tag ->
                                TagChip(tagName = tag)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 工具列與顏色選擇器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "顏色",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NOTE_COLORS.forEach { opt ->
                            val isSelected = selectedColorHex == opt.hex
                            val circleColor = if (opt.hex.isEmpty()) MaterialTheme.colorScheme.surfaceVariant else if (isDark) opt.darkColor else opt.lightColor

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(circleColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = opt.hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isChecklist) "${checklistItems.size} 項" else "${contentValue.text.length} 字",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
