package com.example.myTools.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlin.math.abs

data class TagColor(
    val containerColor: Color,
    val contentColor: Color
)

val TAG_COLOR_PALETTE = listOf(
    TagColor(Color(0xFFE8DEF8), Color(0xFF1D192B)), // 柔紫
    TagColor(Color(0xFFD3E3FD), Color(0xFF041E49)), // 晴藍
    TagColor(Color(0xFFC2F0C2), Color(0xFF0A380A)), // 薄荷綠
    TagColor(Color(0xFFFFDBCF), Color(0xFF380D00)), // 珊瑚橙
    TagColor(Color(0xFFFFF0B3), Color(0xFF332A00)), // 檸檬黃
    TagColor(Color(0xFFFFD8EC), Color(0xFF311027)), // 櫻花粉
    TagColor(Color(0xFFC7F0F0), Color(0xFF003737)), // 湖水綠
    TagColor(Color(0xFFE2E2E2), Color(0xFF1B1B1B))  // 高雅灰
)

fun getTagColor(tagName: String): TagColor {
    val cleanName = tagName.removePrefix("#").trim()
    val index = abs(cleanName.hashCode()) % TAG_COLOR_PALETTE.size
    return TAG_COLOR_PALETTE[index]
}

@Composable
fun TagChip(
    tagName: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val tagColor = getTagColor(tagName)
    val displayText = if (tagName.startsWith("#")) tagName else "#$tagName"

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primary else tagColor.containerColor,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else tagColor.contentColor,
            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = modifier
        ) {
            TagChipContent(displayText = displayText, onDelete = onDelete)
        }
    } else {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primary else tagColor.containerColor,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else tagColor.contentColor,
            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = modifier
        ) {
            TagChipContent(displayText = displayText, onDelete = onDelete)
        }
    }
}

@Composable
private fun TagChipContent(
    displayText: String,
    onDelete: (() -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        if (onDelete != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "刪除標籤",
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onDelete() }
            )
        }
    }
}

@Composable
fun TagFilterRow(
    allTags: List<String>,
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (allTags.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedTag == null,
            onClick = { onTagSelected(null) },
            label = { Text("全部") },
            leadingIcon = if (selectedTag == null) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
            } else null
        )

        allTags.forEach { tag ->
            val isSelected = selectedTag == tag
            TagChip(
                tagName = tag,
                isSelected = isSelected,
                onClick = { onTagSelected(if (isSelected) null else tag) }
            )
        }
    }
}

@Composable
fun ManageTagsDialog(
    title: String,
    currentTags: List<String>,
    allAppTags: List<String>,
    onDismiss: () -> Unit,
    onSaveTags: (List<String>) -> Unit
) {
    var newTagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(currentTags) }

    val addPendingTag = {
        val clean = newTagInput.removePrefix("#").trim()
        if (clean.isNotBlank() && !tags.contains(clean)) {
            tags = tags + clean
            newTagInput = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "已設定標籤：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (tags.isEmpty()) {
                    Text(
                        text = "暫無標籤",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            TagChip(
                                tagName = tag,
                                onDelete = {
                                    tags = tags.filter { it != tag }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    label = { Text("新增標籤") },
                    placeholder = { Text("輸入標籤名稱，例如：親友") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addPendingTag() }),
                    trailingIcon = {
                        IconButton(onClick = addPendingTag) {
                            Icon(Icons.Default.Add, contentDescription = "添加標籤")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (allAppTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "常用標籤：",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allAppTags.forEach { tag ->
                            val isAdded = tags.contains(tag)
                            FilterChip(
                                selected = isAdded,
                                onClick = {
                                    tags = if (isAdded) {
                                        tags.filter { it != tag }
                                    } else {
                                        tags + tag
                                    }
                                },
                                label = { Text("#$tag") },
                                leadingIcon = if (isAdded) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                var finalTags = tags
                val pendingTag = newTagInput.removePrefix("#").trim()
                if (pendingTag.isNotBlank() && !finalTags.contains(pendingTag)) {
                    finalTags = finalTags + pendingTag
                }
                onSaveTags(finalTags.distinct())
                onDismiss()
            }) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
