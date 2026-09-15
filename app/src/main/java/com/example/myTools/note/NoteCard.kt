package com.example.myTools.note

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myTools.ui.TagChip

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteRecord,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onManageTags: () -> Unit,
    onSelectTagFilter: (String) -> Unit,
    onSaveAsTxt: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val colorOpt = NOTE_COLORS.find { it.hex == note.colorHex }
    val cardBg = when {
        colorOpt != null && colorOpt.hex.isNotEmpty() -> if (isDark) colorOpt.darkColor else colorOpt.lightColor
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    var cardMenuExpanded by remember { mutableStateOf(false) }
    val effectiveTags = remember(note.title, note.content, note.tags) { note.getEffectiveTags() }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { cardMenuExpanded = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = cardBg,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (colorOpt != null && colorOpt.hex.isNotEmpty()) {
                if (isDark) {
                    colorOpt.lightColor.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                }
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    val fallbackTitle = if (note.isChecklist && note.checklistItems.isNotEmpty()) {
                        note.checklistItems.firstOrNull { it.text.isNotBlank() }?.text ?: "無標題"
                    } else {
                        note.content.take(20).ifBlank { "無標題" }
                    }
                    Text(
                        text = fallbackTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "取消置頂",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    Box {
                        IconButton(
                            onClick = { cardMenuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "更多選項",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = cardMenuExpanded,
                            onDismissRequest = { cardMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (note.isPinned) "取消置頂" else "置頂筆記") },
                                onClick = {
                                    cardMenuExpanded = false
                                    onTogglePin()
                                },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                            )

                            DropdownMenuItem(
                                text = { Text("標籤分類") },
                                onClick = {
                                    cardMenuExpanded = false
                                    onManageTags()
                                },
                                leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null) }
                            )

                            DropdownMenuItem(
                                text = { Text("複製內容") },
                                onClick = {
                                    cardMenuExpanded = false
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val copyText = if (note.title.isNotBlank()) "${note.title}\n\n${note.content}" else note.content
                                    val clip = ClipData.newPlainText("Note Content", copyText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "已複製筆記內容到剪貼簿", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                            )

                            DropdownMenuItem(
                                text = { Text("另存為 .txt 文件") },
                                onClick = {
                                    cardMenuExpanded = false
                                    onSaveAsTxt()
                                },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                            )

                            DropdownMenuItem(
                                text = { Text("編輯") },
                                onClick = {
                                    cardMenuExpanded = false
                                    onClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("刪除") },
                                onClick = {
                                    cardMenuExpanded = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            if (note.isChecklist && note.checklistItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val previewItems = note.checklistItems.take(4)
                val totalCount = note.checklistItems.size
                val checkedCount = note.checklistItems.count { it.isChecked }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    previewItems.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = item.text.ifBlank { "清單項目" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (item.isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (totalCount > 4 || checkedCount > 0) {
                        val extraText = buildString {
                            if (totalCount > 4) append("... 共 $totalCount 項 ")
                            if (checkedCount > 0) append("($checkedCount 個勾選)")
                        }.trim()
                        if (extraText.isNotEmpty()) {
                            Text(
                                text = extraText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            if (effectiveTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    effectiveTags.forEach { tag ->
                        TagChip(
                            tagName = tag,
                            onClick = { onSelectTagFilter(tag) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = note.getFormattedDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
