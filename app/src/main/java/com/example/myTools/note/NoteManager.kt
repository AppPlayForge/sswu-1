package com.example.myTools.note

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NoteManager {
    private const val PREF_NAME = "note_prefs"
    private const val KEY_LIST = "note_list"
    private const val KEY_TRASH_LIST = "note_trash_list"
    private const val KEY_IS_GRID_VIEW = "is_grid_view"
    private val gson = Gson()
    private val noteListType = object : TypeToken<List<NoteRecord>>() {}.type

    fun isGridView(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_GRID_VIEW, true)
    }

    fun setGridView(context: Context, isGridView: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_IS_GRID_VIEW, isGridView) }
    }

    fun loadList(context: Context): List<NoteRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return try {
            gson.fromJson<List<NoteRecord>>(json, noteListType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveList(context: Context, list: List<NoteRecord>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit { putString(KEY_LIST, json) }
    }

    fun loadTrashList(context: Context): List<NoteRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TRASH_LIST, null) ?: return emptyList()
        return try {
            gson.fromJson<List<NoteRecord>>(json, noteListType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveTrashList(context: Context, list: List<NoteRecord>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit { putString(KEY_TRASH_LIST, json) }
    }

    /**
     * 將筆記移至垃圾桶 (軟刪除)
     */
    fun moveToTrash(context: Context, noteId: Long) {
        val activeList = loadList(context).toMutableList()
        val targetIndex = activeList.indexOfFirst { it.id == noteId }
        if (targetIndex != -1) {
            val item = activeList.removeAt(targetIndex)
            val trashedItem = item.copy(deletedAt = System.currentTimeMillis())
            saveList(context, activeList)

            val trashList = loadTrashList(context).toMutableList()
            trashList.add(0, trashedItem)
            saveTrashList(context, trashList)
        }
    }

    /**
     * 從垃圾桶恢復筆記
     */
    fun restoreFromTrash(context: Context, noteId: Long) {
        val trashList = loadTrashList(context).toMutableList()
        val targetIndex = trashList.indexOfFirst { it.id == noteId }
        if (targetIndex != -1) {
            val item = trashList.removeAt(targetIndex)
            val restoredItem = item.copy(deletedAt = 0L, updatedAt = System.currentTimeMillis())
            saveTrashList(context, trashList)

            val activeList = loadList(context).toMutableList()
            activeList.add(0, restoredItem)
            saveList(context, activeList)
        }
    }

    /**
     * 從垃圾桶永久刪除單筆筆記
     */
    fun permanentlyDeleteFromTrash(context: Context, noteId: Long) {
        val trashList = loadTrashList(context).filter { it.id != noteId }
        saveTrashList(context, trashList)
    }

    /**
     * 清空垃圾桶
     */
    fun emptyTrash(context: Context) {
        saveTrashList(context, emptyList())
    }

    fun addOrUpdateRecord(context: Context, record: NoteRecord): NoteRecord {
        val list = loadList(context).toMutableList()
        val index = list.indexOfFirst { it.id == record.id }
        val updated = record.copy(updatedAt = System.currentTimeMillis())
        if (index != -1) {
            list[index] = updated
        } else {
            list.add(0, updated) // 新筆記置頂
        }
        saveList(context, list)
        return updated
    }

    fun togglePinRecord(context: Context, id: Long) {
        val list = loadList(context).toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = list[index]
            list[index] = item.copy(isPinned = !item.isPinned, updatedAt = System.currentTimeMillis())
            saveList(context, list)
        }
    }

    /**
     * 將單個筆記以 Windows TXT 格式另存到手機下載文件夾 (Downloads)
     */
    fun saveNoteToDownloads(context: Context, note: NoteRecord): String? {
        val rawTitle = note.title.ifBlank { "未命名筆記" }
        val sanitizedTitle = rawTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(note.updatedAt))
        val fileName = "${sanitizedTitle}_$dateSuffix.txt"
        val textContent = note.toTxtString()

        return writeTextFileToDownloads(context, fileName, textContent)
    }

    /**
     * 付費用戶功能：導出筆記本全部內容為全量 TXT 備份文件到下載文件夾
     */
    fun exportAllNotesToDownloads(context: Context): String? {
        val notes = loadList(context)
        if (notes.isEmpty()) return null

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "記事本備份_$dateStr.txt"

        val sb = StringBuilder()
        sb.append("# ==========================================\r\n")
        sb.append("# 記事本全量備份文件 (Notebook Backup)\r\n")
        sb.append("# 導出時間: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())).append("\r\n")
        sb.append("# 筆記總數: ").append(notes.size).append("\r\n")
        sb.append("# 說明: 此文件兼容 Windows 記事本 (UTF-8 格式)\r\n")
        sb.append("# ==========================================\r\n\r\n")

        notes.forEachIndexed { index, note ->
            sb.append("# NOTE_START: ").append(note.title.ifBlank { "未命名筆記 ${index + 1}" }).append("\r\n")
            sb.append("# CREATED: ").append(note.createdAt).append("\r\n")
            sb.append("# UPDATED: ").append(note.updatedAt).append("\r\n")
            note.colorHex?.let { color ->
                sb.append("# COLOR: ").append(color).append("\r\n")
            }
            sb.append("# CONTENT:\r\n")
            val formattedContent = note.content.replace("\r\n", "\n").replace("\n", "\r\n")
            sb.append(formattedContent).append("\r\n")
            sb.append("# NOTE_END\r\n\r\n")
        }

        return writeTextFileToDownloads(context, fileName, sb.toString())
    }

    /**
     * 通用 MediaStore 寫入 Downloads 目錄輔助函式
     */
    private fun writeTextFileToDownloads(context: Context, fileName: String, textContent: String): String? {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(textContent.toByteArray(Charsets.UTF_8))
                }
                fileName
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 從 TXT 文字內容導入筆記本 (解析全量備份標籤，或解析單個普通 Windows txt 檔案)
     */
    fun importNotesFromTxt(context: Context, rawTxtContent: String, defaultTitle: String = "匯入筆記"): Int {
        val cleanTxt = rawTxtContent.replace("\uFEFF", "") // 去除 BOM
        val existingNotes = loadList(context).toMutableList()
        val importedCount: Int

        if (cleanTxt.contains("# NOTE_START:")) {
            // 解析全量備份格式
            val noteBlocks = cleanTxt.split("# NOTE_START:")
            var count = 0
            noteBlocks.forEach { block ->
                val trimmed = block.trim()
                if (trimmed.isEmpty() || !trimmed.contains("# NOTE_END")) return@forEach

                val lines = trimmed.lines()
                val title = lines.firstOrNull()?.substringBefore("# NOTE_END")?.trim() ?: defaultTitle
                
                var createdAt = System.currentTimeMillis()
                var updatedAt = System.currentTimeMillis()
                var colorHex: String? = null
                val contentBuilder = StringBuilder()
                var readingContent = false

                lines.drop(1).forEach { line ->
                    if (line.trim() == "# NOTE_END") {
                        readingContent = false
                        return@forEach
                    }
                    if (readingContent) {
                        if (contentBuilder.isNotEmpty()) contentBuilder.append("\n")
                        contentBuilder.append(line)
                    } else {
                        when {
                            line.startsWith("# CREATED:") -> createdAt = line.substringAfter("# CREATED:").trim().toLongOrNull() ?: createdAt
                            line.startsWith("# UPDATED:") -> updatedAt = line.substringAfter("# UPDATED:").trim().toLongOrNull() ?: updatedAt
                            line.startsWith("# COLOR:") -> colorHex = line.substringAfter("# COLOR:").trim()
                            line.startsWith("# CONTENT:") -> readingContent = true
                        }
                    }
                }

                val newNote = NoteRecord(
                    id = System.currentTimeMillis() + count,
                    title = title,
                    content = contentBuilder.toString(),
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    colorHex = colorHex,
                )
                existingNotes.add(0, newNote)
                count++
            }
            importedCount = count
        } else {
            // 解析普通 Windows TXT 檔案 (第一行可作為標題，其餘為內容)
            val normalized = cleanTxt.replace("\r\n", "\n")
            val lines = normalized.lines()
            val firstLine = lines.firstOrNull()?.trim() ?: ""

            val (title, content) = if (firstLine.isNotEmpty() && firstLine.length <= 60 && lines.size > 1) {
                Pair(firstLine, lines.asSequence().drop(1).joinToString("\n").trim())
            } else if (firstLine.isNotEmpty() && lines.size == 1) {
                Pair(defaultTitle, firstLine)
            } else {
                Pair(defaultTitle, normalized.trim())
            }

            val newNote = NoteRecord(
                id = System.currentTimeMillis(),
                title = title,
                content = content,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            existingNotes.add(0, newNote)
            importedCount = 1
        }

        if (importedCount > 0) {
            saveList(context, existingNotes)
        }
        return importedCount
    }
}
