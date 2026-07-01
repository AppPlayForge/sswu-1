package com.example.myTools.birthday

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import androidx.core.content.edit

object BirthdayManager {
    private const val PREF_NAME = "birthday_prefs"
    private const val KEY_LIST = "birthday_list"
    private val gson = Gson()

    fun loadList(context: Context): List<BirthdayRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LIST, null) ?: return emptyList()
        val type = object : TypeToken<List<BirthdayRecord>>() {}.type
        return try {
            val rawList: List<BirthdayRecord>? = gson.fromJson(json, type)
            rawList ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun saveList(context: Context, list: List<BirthdayRecord>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit { putString(KEY_LIST, json) }
    }

    fun addOrUpdateRecord(context: Context, record: BirthdayRecord) {
        val list = loadList(context).toMutableList()
        val index = list.indexOfFirst { it.id == record.id }
        
        // 如果是編輯，先取消舊的鬧鐘（注意：這裡取消邏輯可能需要兼容舊版）
        // 由於我們更改了數據結構，最穩妥的方法是取消所有可能的舊鬧鐘組合
        cancelOldAlarms(context, record.id)
        
        if (index != -1) {
            list[index] = record
        } else {
            list.add(record)
        }
        
        saveList(context, list)
        // 設定新的鬧鐘
        scheduleBirthdayAlarm(context, record)
    }

    fun deleteRecord(context: Context, id: Long) {
        val list = loadList(context)
        val recordToRemove = list.find { it.id == id }
        if (recordToRemove != null) {
            cancelAlarm(context, recordToRemove)
            val newList = list.filter { it.id != id }
            saveList(context, newList)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleBirthdayAlarm(context: Context, record: BirthdayRecord) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        record.remindList.forEach { daysBefore ->
            val nextBirthdayCal = getNextBirthdayCalendar(record.lunarMonth, record.lunarDay)
            val reminderCal = nextBirthdayCal.clone() as Calendar
            reminderCal.add(Calendar.DAY_OF_YEAR, -daysBefore)
            reminderCal.set(Calendar.HOUR_OF_DAY, record.remindHour)
            reminderCal.set(Calendar.MINUTE, record.remindMinute)
            reminderCal.set(Calendar.SECOND, 0)
            reminderCal.set(Calendar.MILLISECOND, 0)

            if (reminderCal.timeInMillis < System.currentTimeMillis()) {
                // 如果今年的提醒時間已過，則不設置（或者可以考慮設置明年的，但通常明年會重新觸發）
                return@forEach
            }
            
            val msg = when (daysBefore) {
                0 -> "今天是 ${record.name} 的農曆生日！"
                1 -> "明天是 ${record.name} 的農曆生日"
                else -> "${record.name} 的農曆生日還有 $daysBefore 天"
            }
            
            // 唯一請求碼：ID後5位 + 天數*100000 + 小時*100 + 分鐘
            val uniqueRequestCode = (record.id % 100000).toInt() + (daysBefore * 100000)
            
            val intent = Intent(context, BirthdayReceiver::class.java).apply {
                putExtra("name", record.name)
                putExtra("message", msg)
                putExtra("id", uniqueRequestCode)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                uniqueRequestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderCal.timeInMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderCal.timeInMillis, pendingIntent)
            }
        }
    }

    fun cancelAlarm(context: Context, record: BirthdayRecord) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BirthdayReceiver::class.java)
        
        // 取消當前記錄關聯的所有提醒天數的鬧鐘
        // 注意：我們之前的 uniqueRequestCode 邏輯發生了變化，這裡儘量覆蓋
        record.remindList.forEach { daysBefore ->
            val uniqueRequestCode = (record.id % 100000).toInt() + (daysBefore * 100000)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                uniqueRequestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        // 同時嘗試取消可能存在的舊版鬧鐘組合
        cancelOldAlarms(context, record.id)
    }

    private fun cancelOldAlarms(context: Context, recordId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BirthdayReceiver::class.java)
        val allPossibleDays = listOf(0, 1, 3, 7)
        val allPossibleHours = listOf(9, 14, 19)
        
        allPossibleDays.forEach { d ->
            allPossibleHours.forEach { h ->
                val uniqueRequestCode = (recordId % 100000).toInt() + (d * 100000) + (h * 100)
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    uniqueRequestCode, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}
