package com.example.myTools.note

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val isChecked: Boolean = false
)

data class NoteRecord(
    val id: Long = System.currentTimeMillis(),
    val title: String = "",
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val colorHex: String? = null,
    val tags: List<String> = emptyList(),
    val deletedAt: Long = 0L,
    val isChecklist: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList()
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(updatedAt))
    }

    /**
     * 從標題與內容自動提取 #標籤 並合併顯式標籤
     */
    fun getEffectiveTags(): List<String> {
        val explicit = tags
        val fullText = if (isChecklist) {
            val itemsText = checklistItems.joinToString(" ") { it.text }
            "$title $itemsText $content"
        } else {
            "$title $content"
        }
        val extracted = extractHashtags(fullText)
        return (explicit + extracted).distinct()
    }

    /**
     * 轉換為兼容 Windows 記事本的 TXT 文字 (UTF-8, CRLF 換行)
     */
    fun toTxtString(): String {
        val sb = StringBuilder()
        if (title.isNotBlank()) {
            sb.append(title.trim()).append("\r\n\r\n")
        }
        if (isChecklist && checklistItems.isNotEmpty()) {
            checklistItems.forEach { item ->
                val checkSymbol = if (item.isChecked) "[v]" else "[ ]"
                sb.append("$checkSymbol ${item.text}").append("\r\n")
            }
            if (content.isNotBlank()) {
                sb.append("\r\n").append(content.replace("\r\n", "\n").replace("\n", "\r\n"))
            }
        } else {
            val normalizedContent = content.replace("\r\n", "\n").replace("\n", "\r\n")
            sb.append(normalizedContent)
        }
        return sb.toString()
    }

    companion object {
        fun extractHashtags(text: String): List<String> {
            if (text.isBlank()) return emptyList()
            val regex = Regex("""#([^\s#,.!?;:，。！？；：/\\]+)""")
            return regex.findAll(text)
                .map { it.groupValues[1].trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toList()
        }
    }
}
