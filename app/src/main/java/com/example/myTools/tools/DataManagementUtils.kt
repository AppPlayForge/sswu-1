package com.example.myTools.tools

import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import com.example.myTools.bazi.BaZiManager
import com.example.myTools.bazi.BaZiRecord
import com.example.myTools.birthday.BirthdayManager
import com.example.myTools.birthday.BirthdayRecord
import com.example.myTools.period.PeriodDataManager
import com.example.myTools.period.PeriodRecord
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

object DataManagementUtils {
    private const val PREF_NAME = "activation_prefs"
    private const val KEY_ACTIVATION_CODE = "activation_code"
    
    // 激活邏輯：根據設備 ID 生成
    private const val SALT = "SSWU_SALT_2026"

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
    }

    private fun generateValidCode(deviceId: String): String {
        val input = deviceId + SALT
        val hash = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        val hex = hash.joinToString("") { "%02x".format(it) }.uppercase()
        return "SSWU-${hex.take(8)}-${hex.takeLast(4)}"
    }

    fun isActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_ACTIVATION_CODE, "") ?: ""
        return code == generateValidCode(getDeviceId(context))
    }

    fun activate(context: Context, code: String): Boolean {
        if (code == generateValidCode(getDeviceId(context))) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit { putString(KEY_ACTIVATION_CODE, code) }
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
            val duration = if (r.endDate != null) ((r.endDate - r.startDate) / (24 * 60 * 60 * 1000) + 1).toString() else ""
            sb.append("$start,$end,$duration\n")
        }

        return sb.toString()
    }

    /**
     * 從 CSV 導入全量數據
     */
    fun importAllFromCsv(context: Context, csv: String): Boolean {
        return try {
            val lines = csv.replace("\uFEFF", "").lines()
            val baziList = mutableListOf<BaZiRecord>()
            val birthdayList = mutableListOf<BirthdayRecord>()
            val periodList = mutableListOf<PeriodRecord>()
            
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
                            baziList.add(BaZiRecord(
                                id = parts[0].toLong(),
                                surname = "", // CSV 導出時合併了姓名，導入時放在 givenName
                                givenName = parts[1],
                                gender = parts[2],
                                year = parts[3].toInt(),
                                month = parts[4].toInt(),
                                day = parts[5].toInt(),
                                hour = parts[6].toInt(),
                                minute = parts[7].toInt(),
                                province = parts[8],
                                city = parts[9],
                                isLunar = parts[10].toBoolean(),
                                isLeapMonth = parts[11].toBoolean()
                            ))
                        }
                    }
                    "BIRTHDAY" -> {
                        if (parts.size >= 7) {
                            birthdayList.add(BirthdayRecord(
                                id = parts[0].toLong(),
                                name = parts[1],
                                lunarMonth = parts[2].toInt(),
                                lunarDay = parts[3].toInt(),
                                remindHour = parts[4].toInt(),
                                remindMinute = parts[5].toInt(),
                                remindList = parts[6].split(";").filter { it.isNotEmpty() }.map { it.toInt() }
                            ))
                        }
                    }
                    "PERIOD" -> {
                        if (parts.size >= 2) {
                            val start = sdf.parse(parts[0])?.time ?: return@forEach
                            val end = if (parts[1].isNotEmpty()) sdf.parse(parts[1])?.time else null
                            periodList.add(PeriodRecord(start, end))
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

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
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
        var current = StringBuilder()
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
                current = StringBuilder()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
