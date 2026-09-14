package com.example.myTools.note

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object TextFileLoader {
    // 最大檔案限制 5MB，防止開啟極大檔案造成記憶體溢出 (OOM) 或 App 卡死
    private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L

    sealed class Result {
        data class Success(val fileName: String, val content: String) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * 安全地檢測並讀取 Uri 對應的文本檔案
     */
    fun readTextFromUri(context: Context, uri: Uri): Result {
        return try {
            val contentResolver = context.contentResolver

            // 1. 取得檔案名稱與大小
            var fileSize = 0L
            var fileName = "文字檔案.txt"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { fileName = it }
                    }
                }
            }

            // 檔案過大攔截
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return Result.Error("檔案大小超過 5MB 限制，無法開啟")
            }

            // 2. 副檔名與 MIME 類型過濾 (已知多媒體/二進制檔案排他檢查)
            val mimeType = contentResolver.getType(uri) ?: ""
            if (isKnownBinaryExtension(fileName) || isKnownBinaryMimeType(mimeType)) {
                return Result.Error("「$fileName」為非文本格式 (多媒體/圖片/壓縮檔)，無法開啟")
            }

            // 3. 讀取前 8KB 緩衝區，檢查是否含有 NUL 控制字元 (\u0000) 等二進制特徵
            val isBinary = contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArray(8192)
                val bytesRead = inputStream.read(buffer)
                if (bytesRead > 0) {
                    isBinaryContent(buffer, bytesRead)
                } else false
            } ?: false

            if (isBinary) {
                return Result.Error("「$fileName」包含二進制字元（非純文字），無法開啟")
            }

            // 4. 正式讀取完整文字內容 (UTF-8)
            val textContent = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return Result.Error("讀取檔案內容失敗")

            Result.Success(fileName, textContent)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error("讀取檔案失敗：${e.localizedMessage ?: "未知錯誤"}")
        }
    }

    /**
     * 檢測字元緩衝區是否含有 NUL (\u0000) 或過多非列印控制字元
     */
    private fun isBinaryContent(buffer: ByteArray, length: Int): Boolean {
        var nullCount = 0
        var nonPrintableCount = 0
        for (i in 0 until length) {
            val byte = buffer[i].toInt() and 0xFF
            if (byte == 0) {
                nullCount++
                if (nullCount > 1) return true
            }
            // 控制字元檢測（排除常用 TAB \t, LF \n, CR \r）
            if ((byte in 0..6) || (byte in 14..31 && byte != 27)) {
                nonPrintableCount++
            }
        }
        return nullCount > 0 || (nonPrintableCount > length * 0.08)
    }

    private fun isKnownBinaryExtension(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return false
        val binaryExts = setOf(
            // 音訊與影片
            "mp3", "wav", "aac", "flac", "ogg", "m4a", "wma", "amr",
            "mp4", "mkv", "avi", "mov", "wmv", "3gp", "flv", "webm",
            // 圖片
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "ico", "tiff", "svgz",
            // 壓縮檔與安裝包
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "apk", "aab", "iso", "dmg",
            // 二進制文件與數據庫
            "exe", "dll", "so", "bin", "class", "dex", "dat", "db", "sqlite", "dylib",
            // Office 密閉二進制格式
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
        )
        return binaryExts.contains(ext)
    }

    private fun isKnownBinaryMimeType(mimeType: String): Boolean {
        if (mimeType.isEmpty()) return false
        val lower = mimeType.lowercase()
        return lower.startsWith("image/") ||
                lower.startsWith("audio/") ||
                lower.startsWith("video/") ||
                lower.startsWith("font/") ||
                lower.contains("octet-stream") ||
                lower.contains("pdf") ||
                lower.contains("zip") ||
                lower.contains("compressed")
    }
}
