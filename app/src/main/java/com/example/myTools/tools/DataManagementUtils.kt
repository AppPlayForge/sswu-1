package com.example.myTools.tools

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import com.example.myTools.bazi.BaZiManager
import com.example.myTools.bazi.BaZiRecord
import com.example.myTools.birthday.BirthdayManager
import com.example.myTools.birthday.BirthdayRecord
import com.example.myTools.note.NoteManager
import com.example.myTools.note.NoteRecord
import com.example.myTools.period.PeriodDataManager
import com.example.myTools.period.PeriodRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataManagementUtils {
    private const val PREF_NAME = "activation_prefs"
    private const val KEY_ACTIVATION_CODE = "activation_code"

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
    }

    fun generateValidCode(deviceId: String): String {
        return ActivationSecret.generateValidCode(deviceId)
    }

    fun isActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_ACTIVATION_CODE, "") ?: ""
        return ActivationSecret.verifyCode(getDeviceId(context), code)
    }

    fun activate(context: Context, code: String): Boolean {
        val trimmedCode = code.trim()
        if (ActivationSecret.verifyCode(getDeviceId(context), trimmedCode)) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit { putString(KEY_ACTIVATION_CODE, trimmedCode) }
            return true
        }
        return false
    }

    /**
     * 導出全量數據為 CSV (支持 Excel 查看與 App 導入)
     */
    fun exportAllToCsv(context: Context): String {
        val baziList = BaZiManager.loadList(context)
        val birthdayList = BirthdayManager.loadList(context)
        val periodList = PeriodDataManager(context).getRecords()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM

        // --- 八字章節 ---
        sb.append("# SECTION:BAZI\n")
        sb.append("ID,姓名,性別,年,月,日,時,分,省份,城市,是否農曆,是否閏月\n")
        baziList.forEach { r ->
            sb.append("${r.id},${escapeCsv(r.name)},${r.gender},${r.year},${r.month},${r.day},${r.hour},${r.minute},${escapeCsv(r.province)},${escapeCsv(r.city)},${r.isLunar},${r.isLeapMonth}\n")
        }
        sb.append("\n")

        // --- 生日章節 ---
        sb.append("# SECTION:BIRTHDAY\n")
        sb.append("ID,姓名,農曆月,農曆日,提醒小時,提醒分鐘,提醒清單\n")
        birthdayList.forEach { r ->
            sb.append("${r.id},${escapeCsv(r.name)},${r.lunarMonth},${r.lunarDay},${r.remindHour},${r.remindMinute},\"${r.remindList.joinToString(";")}\"\n")
        }
        sb.append("\n")

        // --- 月經章節 ---
        sb.append("# SECTION:PERIOD\n")
        sb.append("開始日期,結束日期,持續天數\n")
        periodList.forEach { r ->
            val start = sdf.format(Date(r.startDate))
            val end = r.endDate?.let { sdf.format(Date(it)) } ?: ""
            val duration = if (r.endDate != null) (((r.endDate - r.startDate) / PeriodDataManager.DAY_IN_MILLIS) + 1).toString() else ""
            sb.append("$start,$end,$duration\n")
        }
        sb.append("\n")

        // --- 筆記章節 ---
        val noteList = NoteManager.loadList(context)
        sb.append("# SECTION:NOTE\n")
        sb.append("ID,標題,內容,更新時間,創建時間,是否置頂,顏色\n")
        noteList.forEach { r ->
            sb.append("${r.id},${escapeCsv(r.title)},${escapeCsv(r.content)},${r.updatedAt},${r.createdAt},${r.isPinned},${r.colorHex ?: ""}\n")
        }

        return sb.toString()
    }

    /**
     * 從 CSV 導入全量數據
     */
    fun importAllFromCsv(context: Context, csv: String): Boolean {
        return try {
            val lines = getLogicalCsvLines(csv)
            val baziList = mutableListOf<BaZiRecord>()
            val birthdayList = mutableListOf<BirthdayRecord>()
            val periodList = mutableListOf<PeriodRecord>()
            val noteList = mutableListOf<NoteRecord>()

            var currentSection = ""
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach

                if (trimmed.startsWith("# SECTION:")) {
                    currentSection = trimmed.substringAfter("# SECTION:")
                    return@forEach
                }

                // 跳過表頭
                if (trimmed.startsWith("ID") || trimmed.startsWith("開始日期")) return@forEach

                val parts = parseCsvLine(line)
                when (currentSection) {
                    "BAZI" -> {
                        if (parts.size >= 12) {
                            baziList.add(
                                BaZiRecord(
                                    id = parts[0].toLongOrNull() ?: System.currentTimeMillis(),
                                    surname = "", // CSV 導出時合併了姓名，導入時放在 givenName
                                    givenName = parts[1],
                                    gender = parts[2],
                                    year = parts[3].toIntOrNull() ?: 1990,
                                    month = parts[4].toIntOrNull() ?: 1,
                                    day = parts[5].toIntOrNull() ?: 1,
                                    hour = parts[6].toIntOrNull() ?: 12,
                                    minute = parts[7].toIntOrNull() ?: 0,
                                    province = parts[8],
                                    city = parts[9],
                                    isLunar = parts[10].toBoolean(),
                                    isLeapMonth = parts[11].toBoolean(),
                                )
                            )
                        }
                    }
                    "BIRTHDAY" -> {
                        if (parts.size >= 7) {
                            birthdayList.add(
                                BirthdayRecord(
                                    id = parts[0].toLongOrNull() ?: System.currentTimeMillis(),
                                    name = parts[1],
                                    lunarMonth = parts[2].toIntOrNull() ?: 1,
                                    lunarDay = parts[3].toIntOrNull() ?: 1,
                                    remindHour = parts[4].toIntOrNull() ?: 9,
                                    remindMinute = parts[5].toIntOrNull() ?: 0,
                                    remindList = parts[6].split(";").mapNotNull { s -> s.trim().toIntOrNull() },
                                )
                            )
                        }
                    }
                    "PERIOD" -> {
                        if (parts.size >= 2) {
                            val start = try { sdf.parse(parts[0])?.time } catch (_: Exception) { null } ?: return@forEach
                            val end = if (parts[1].isNotEmpty()) {
                                try { sdf.parse(parts[1])?.time } catch (_: Exception) { null }
                            } else null
                            periodList.add(PeriodRecord(start, end))
                        }
                    }
                    "NOTE" -> {
                        if (parts.size >= 5) {
                            noteList.add(
                                NoteRecord(
                                    id = parts[0].toLongOrNull() ?: System.currentTimeMillis(),
                                    title = parts[1],
                                    content = parts[2],
                                    updatedAt = parts[3].toLongOrNull() ?: System.currentTimeMillis(),
                                    createdAt = parts.getOrNull(4)?.toLongOrNull() ?: System.currentTimeMillis(),
                                    isPinned = parts.getOrNull(5)?.toBoolean() ?: false,
                                    colorHex = parts.getOrNull(6)?.ifBlank { null },
                                )
                            )
                        }
                    }
                }
            }

            // 保存數據
            if (baziList.isNotEmpty()) BaZiManager.saveList(context, baziList)
            if (birthdayList.isNotEmpty()) {
                BirthdayManager.saveList(context, birthdayList)
                birthdayList.forEach { BirthdayManager.scheduleBirthdayAlarm(context, it) }
            }
            if (periodList.isNotEmpty()) PeriodDataManager(context).saveRecords(periodList)
            if (noteList.isNotEmpty()) NoteManager.saveList(context, noteList)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 處理包含換行符號的多行 CSV 數據列
     */
    private fun getLogicalCsvLines(csv: String): List<String> {
        val rawLines = csv.replace("\uFEFF", "").lines()
        val result = mutableListOf<String>()
        val currentLine = StringBuilder()
        var inQuotes = false

        for (line in rawLines) {
            val quoteCount = line.count { it == '"' }
            if (currentLine.isNotEmpty()) {
                currentLine.append("\n").append(line)
            } else {
                currentLine.append(line)
            }
            if (quoteCount % 2 != 0) {
                inQuotes = !inQuotes
            }
            if (!inQuotes) {
                result.add(currentLine.toString())
                currentLine.clear()
            }
        }
        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
        }
        return result
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    current.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString())
                current.clear()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
