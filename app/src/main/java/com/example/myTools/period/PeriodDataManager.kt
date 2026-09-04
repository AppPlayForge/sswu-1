package com.example.myTools.period

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

data class PeriodRecord(
    val startDate: Long,
    val endDate: Long? = null
)

class PeriodDataManager(context: Context? = null) {
    private val prefs = context?.getSharedPreferences("period_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getRecords(): List<PeriodRecord> {
        val json = prefs?.getString("records", "[]") ?: "[]"
        val type = object : TypeToken<List<PeriodRecord>>() {}.type
        val records: List<PeriodRecord> = gson.fromJson(json, type) ?: emptyList()
        return records.sortedByDescending { it.startDate }
    }

    fun saveRecords(records: List<PeriodRecord>) {
        val sorted = records.sortedByDescending { it.startDate }
        val json = gson.toJson(sorted)
        prefs?.edit()?.putString("records", json)?.apply()
    }

    fun addRecord(startDate: Long, endDate: Long? = null) {
        val records = getRecords().toMutableList()
        records.add(PeriodRecord(startDate, endDate))
        saveRecords(records)
    }

    fun updateRecord(oldRecord: PeriodRecord, newRecord: PeriodRecord) {
        val records = getRecords().toMutableList()
        val index = records.indexOfFirst {
            it.startDate == oldRecord.startDate && it.endDate == oldRecord.endDate
        }
        if (index != -1) {
            records[index] = newRecord
            saveRecords(records)
        }
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
        records.removeAll { it.startDate == record.startDate && it.endDate == record.endDate }
        saveRecords(records)
    }

    /**
     * 計算平均月經週期長度 (兩次月經開始日之間的間隔天數)
     */
    fun getAverageCycleLength(records: List<PeriodRecord>): Int {
        val sortedRecords = records.sortedBy { it.startDate }
        if (sortedRecords.size < 2) return 28 // 預設 28 天

        var totalDays = 0L
        for (i in 0 until sortedRecords.size - 1) {
            val start1 = normalizeToStartOfDay(sortedRecords[i].startDate)
            val start2 = normalizeToStartOfDay(sortedRecords[i + 1].startDate)
            totalDays += (start2 - start1) / DAY_IN_MILLIS
        }
        val avg = (totalDays / (sortedRecords.size - 1)).toInt()
        return avg.coerceIn(21, 35)
    }

    /**
     * 計算平均經期持續天數 (從開始到結束的天數)
     */
    fun getAveragePeriodLength(records: List<PeriodRecord>): Int {
        val completedRecords = records.filter { it.endDate != null }
        if (completedRecords.isEmpty()) return 5 // 預設 5 天

        var totalDays = 0L
        for (record in completedRecords) {
            val start = normalizeToStartOfDay(record.startDate)
            val end = normalizeToStartOfDay(record.endDate!!)
            val days = (end - start) / DAY_IN_MILLIS + 1
            if (days > 0) {
                totalDays += days
            }
        }
        val avg = (totalDays / completedRecords.size).toInt()
        return avg.coerceIn(3, 10)
    }

    /**
     * 預測下次月經開始日期
     */
    fun predictNextPeriod(records: List<PeriodRecord>): Long? {
        if (records.isEmpty()) return null
        
        val lastStart = normalizeToStartOfDay(records.maxOf { it.startDate })
        val avgLength = getAverageCycleLength(records)
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastStart
        calendar.add(Calendar.DAY_OF_YEAR, avgLength)
        return calendar.timeInMillis
    }

    /**
     * 獲取狀態提示文字
     */
    fun getPeriodStatus(todayMillis: Long, records: List<PeriodRecord>): String {
        if (records.isEmpty()) return "尚無記錄"
        
        val today = normalizeToStartOfDay(todayMillis)
        val sortedAsc = records.sortedBy { it.startDate }
        val last = sortedAsc.last()
        val lastStart = normalizeToStartOfDay(last.startDate)

        // 檢查今天是否在最後一次月經期內
        val lastEnd = last.endDate?.let { normalizeToStartOfDay(it) }
        if (lastEnd != null) {
            if (today in lastStart..lastEnd) {
                val days = (today - lastStart) / DAY_IN_MILLIS + 1
                return "月經第 ${days} 天"
            }
        } else if (today >= lastStart) {
            val days = (today - lastStart) / DAY_IN_MILLIS + 1
            return "月經第 ${days} 天"
        }
        
        val nextMillis = predictNextPeriod(records) ?: return "數據不足"
        val next = normalizeToStartOfDay(nextMillis)
        val daysUntil = (next - today) / DAY_IN_MILLIS
        return when {
            daysUntil > 0 -> "距離下次月經還有 ${daysUntil} 天"
            daysUntil == 0L -> "預計月經今天到來"
            else -> "預計月經已延遲 ${-daysUntil} 天"
        }
    }

    fun normalizeToStartOfDay(millis: Long): Long {
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
    fun getCurrentPhase(dateMillis: Long, records: List<PeriodRecord>): Int {
        if (records.isEmpty()) return 0
        
        val targetDay = normalizeToStartOfDay(dateMillis)
        val sortedAsc = records.sortedBy { it.startDate }
        val avgCycle = getAverageCycleLength(records)
        val avgPeriod = getAveragePeriodLength(records)
        val today = normalizeToStartOfDay(System.currentTimeMillis())

        // Step 1: 檢查是否在已記錄的月經期內
        for (i in sortedAsc.indices) {
            val record = sortedAsc[i]
            val recordStart = normalizeToStartOfDay(record.startDate)
            if (record.endDate != null) {
                val recordEnd = normalizeToStartOfDay(record.endDate)
                if (targetDay in recordStart..recordEnd) return 2
            } else {
                // 如果月經還沒紀錄結束時間
                if (i == sortedAsc.lastIndex) {
                    val autoEnd = maxOf(today, recordStart + (avgPeriod - 1) * DAY_IN_MILLIS)
                    if (targetDay in recordStart..autoEnd) return 2
                } else {
                    val defaultEnd = recordStart + (avgPeriod - 1) * DAY_IN_MILLIS
                    if (targetDay in recordStart..defaultEnd) return 2
                }
            }
        }

        // Step 2: 檢查歷史週期區間 (在第一筆記錄與最新一筆記錄之間)
        for (i in 0 until sortedAsc.size - 1) {
            val startCurrent = normalizeToStartOfDay(sortedAsc[i].startDate)
            val startNext = normalizeToStartOfDay(sortedAsc[i + 1].startDate)
            
            if (targetDay in startCurrent until startNext) {
                val ovulationDay = startNext - 14 * DAY_IN_MILLIS
                val fertileStart = ovulationDay - 5 * DAY_IN_MILLIS
                val fertileEnd = ovulationDay + 3 * DAY_IN_MILLIS
                
                return if (targetDay in fertileStart..fertileEnd) 1 else 0
            }
        }

        // Step 3: 檢查最新記錄之後的日期 (當前週期及未來預測週期)
        val lastStart = normalizeToStartOfDay(sortedAsc.last().startDate)
        if (targetDay >= lastStart) {
            val diffDays = (targetDay - lastStart) / DAY_IN_MILLIS
            val cycleIndex = (diffDays / avgCycle).toInt()
            
            val currentCycleStart = lastStart + cycleIndex * avgCycle * DAY_IN_MILLIS
            val nextCycleStart = currentCycleStart + avgCycle * DAY_IN_MILLIS
            
            if (cycleIndex >= 1) {
                val predEnd = currentCycleStart + (avgPeriod - 1) * DAY_IN_MILLIS
                if (targetDay in currentCycleStart..predEnd) return 3
            }

            val ovulationDay = nextCycleStart - 14 * DAY_IN_MILLIS
            val fertileStart = ovulationDay - 5 * DAY_IN_MILLIS
            val fertileEnd = ovulationDay + 3 * DAY_IN_MILLIS

            return if (targetDay in fertileStart..fertileEnd) 1 else 0
        }

        // Step 4: 第一筆記錄之前的日期
        val firstStart = normalizeToStartOfDay(sortedAsc.first().startDate)
        if (targetDay < firstStart) {
            val daysBefore = (firstStart - targetDay) / DAY_IN_MILLIS
            val cyclesBack = ((daysBefore + avgCycle - 1) / avgCycle).toInt()
            
            val estCycleStart = firstStart - cyclesBack * avgCycle * DAY_IN_MILLIS
            val estNextCycleStart = estCycleStart + avgCycle * DAY_IN_MILLIS

            val ovulationDay = estNextCycleStart - 14 * DAY_IN_MILLIS
            val fertileStart = ovulationDay - 5 * DAY_IN_MILLIS
            val fertileEnd = ovulationDay + 3 * DAY_IN_MILLIS

            return if (targetDay in fertileStart..fertileEnd) 1 else 0
        }

        return 0
    }

    companion object {
        const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
    }
}
