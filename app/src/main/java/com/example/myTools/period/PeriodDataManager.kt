package com.example.myTools.period

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

data class PeriodRecord(
    val startDate: Long,
    val endDate: Long? = null
)

class PeriodDataManager(context: Context) {
    private val prefs = context.getSharedPreferences("period_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getRecords(): List<PeriodRecord> {
        val json = prefs.getString("records", "[]")
        val type = object : TypeToken<List<PeriodRecord>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveRecords(records: List<PeriodRecord>) {
        val json = gson.toJson(records)
        prefs.edit().putString("records", json).apply()
    }

    fun addRecord(startDate: Long) {
        val records = getRecords().toMutableList()
        records.add(PeriodRecord(startDate))
        saveRecords(records.sortedByDescending { it.startDate })
    }

    fun updateLastRecord(endDate: Long) {
        val records = getRecords().toMutableList()
        if (records.isNotEmpty()) {
            val last = records[0]
            if (last.endDate == null) {
                records[0] = last.copy(endDate = endDate)
                saveRecords(records)
            }
        }
    }

    fun deleteRecord(record: PeriodRecord) {
        val records = getRecords().toMutableList()
        records.remove(record)
        saveRecords(records)
    }

    fun getAverageCycleLength(records: List<PeriodRecord>): Int {
        val completedRecords = records.filter { it.endDate != null }.sortedBy { it.startDate }
        if (completedRecords.size < 2) return 28 // Default

        var totalDays = 0L
        for (i in 0 until completedRecords.size - 1) {
            val start1 = normalizeToStartOfDay(completedRecords[i].startDate)
            val start2 = normalizeToStartOfDay(completedRecords[i + 1].startDate)
            totalDays += (start2 - start1) / (24 * 60 * 60 * 1000)
        }
        return (totalDays / (completedRecords.size - 1)).toInt().coerceIn(21, 35)
    }

    fun predictNextPeriod(records: List<PeriodRecord>): Long? {
        if (records.isEmpty()) return null
        
        val lastStart = records.sortedByDescending { it.startDate }[0].startDate
        val avgLength = getAverageCycleLength(records)
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastStart
        calendar.add(Calendar.DAY_OF_YEAR, avgLength)
        return calendar.timeInMillis
    }

    fun getPeriodStatus(todayMillis: Long, records: List<PeriodRecord>): String {
        if (records.isEmpty()) return "尚無記錄"
        
        val today = normalizeToStartOfDay(todayMillis)
        val sortedRecords = records.sortedByDescending { it.startDate }
        val last = sortedRecords[0]
        val lastStart = normalizeToStartOfDay(last.startDate)

        if (last.endDate == null) {
            val days = (today - lastStart) / (24 * 60 * 60 * 1000) + 1
            return if (days > 0) "月經第 ${days} 天" else "尚未開始"
        }
        
        val nextMillis = predictNextPeriod(records) ?: return "數據不足"
        val next = normalizeToStartOfDay(nextMillis)
        val daysUntil = (next - today) / (24 * 60 * 60 * 1000)
        return if (daysUntil > 0) "距離下次月經還有 ${daysUntil} 天" else "預計月經即將到來"
    }

    private fun normalizeToStartOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * 計算安全期、排卵期等
     * 返回當前所處階段：0: 安全期, 1: 排卵期(易孕期), 2: 月經期, 3: 預測月經期
     */
    fun getCurrentPhase(todayMillis: Long, records: List<PeriodRecord>): Int {
        if (records.isEmpty()) return 0
        
        val today = normalizeToStartOfDay(todayMillis)
        val sortedRecords = records.sortedByDescending { it.startDate }
        
        // 檢查是否在已記錄的月經期內 (按天比較)
        for (record in sortedRecords) {
            val recordStart = normalizeToStartOfDay(record.startDate)
            if (record.endDate != null) {
                val recordEnd = normalizeToStartOfDay(record.endDate)
                if (today in recordStart..recordEnd) return 2
            } else {
                // 如果月經還沒結束，則從開始日到今天(或無限)都算月經期
                if (today >= recordStart) return 2
            }
        }
        
        val lastStart = normalizeToStartOfDay(sortedRecords[0].startDate)
        val avgLength = getAverageCycleLength(records)
        
        // 計算相對於最後一次月經開始的天數
        val diffMillis = today - lastStart
        val diffDays = (diffMillis / (24 * 60 * 60 * 1000)).toInt()
        val dayInCycle = if (diffDays >= 0) (diffDays % avgLength) + 1 else (diffDays % avgLength + avgLength) % avgLength + 1
        
        val lastEnd = sortedRecords[0].endDate?.let { normalizeToStartOfDay(it) } ?: lastStart
        
        // 預測月經期 (假設持續 5 天)
        // 只有在當前日期晚於最後一次月經結束日期時才進行預測
        if (dayInCycle in 1..5 && today > lastEnd) return 3
        
        // 簡單估算：排卵日為下次月經前14天
        val ovulationDay = avgLength - 14
        val fertileStart = ovulationDay - 5
        val fertileEnd = ovulationDay + 3
        
        return if (dayInCycle in fertileStart..fertileEnd) 1 else 0
    }
}
