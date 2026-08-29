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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

data class AppDataExport(
    val baziList: List<BaZiRecord>,
    val birthdayList: List<BirthdayRecord>,
    val periodList: List<PeriodRecord>,
    val exportTime: Long = System.currentTimeMillis()
)

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

    fun exportDataToJson(context: Context): String {
        val baziList = BaZiManager.loadList(context)
        val birthdayList = BirthdayManager.loadList(context)
        val periodList = PeriodDataManager(context).getRecords()
        val export = AppDataExport(baziList, birthdayList, periodList)
        return Gson().toJson(export)
    }

    fun importDataFromJson(context: Context, json: String): Boolean {
        return try {
            val type = object : TypeToken<AppDataExport>() {}.type
            val data: AppDataExport = Gson().fromJson(json, type)
            
            // 導入八字
            BaZiManager.saveList(context, data.baziList)
            
            // 導入生日並重新設置鬧鐘
            BirthdayManager.saveList(context, data.birthdayList)
            data.birthdayList.forEach { record ->
                BirthdayManager.scheduleBirthdayAlarm(context, record)
            }

            // 導入月經記錄
            PeriodDataManager(context).saveRecords(data.periodList)
            
            true
        } catch (e: Exception) {
            false
        }
    }

    // 導出為 Excel 兼容的 CSV 格式（針對月經記錄）
    fun exportPeriodToCsv(context: Context): String {
        val records = PeriodDataManager(context).getRecords()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        
        // CSV Header (UTF-8 with BOM for Excel compatibility)
        sb.append('\uFEFF')
        sb.append("開始時間,結束時間,持續天數\n")
        
        records.forEach { record ->
            val start = sdf.format(Date(record.startDate))
            val end = record.endDate?.let { sdf.format(Date(it)) } ?: "進行中"
            val duration = if (record.endDate != null) {
                ((record.endDate - record.startDate) / (24 * 60 * 60 * 1000) + 1).toString()
            } else {
                "N/A"
            }
            sb.append("$start,$end,$duration\n")
        }
        return sb.toString()
    }
}
