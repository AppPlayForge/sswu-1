package com.example.myTools.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.os.Build
import android.widget.RemoteViews
import com.example.myTools.MainActivity
import com.example.myTools.R
import com.nlf.calendar.Lunar
import java.util.Calendar
import java.util.Date


//桌面小部件

/**
 * 1.引入午夜鬧鐘機制：在 LunarWidgetProvider 中添加了 scheduleMidnightUpdate 方法，利用 AlarmManager 預約下一個 0 點的精確廣播。
 * 2.處理系統啟動廣播：在 AndroidManifest.xml 中添加了 RECEIVE_BOOT_COMPLETED 權限和 BOOT_COMPLETED 動作監聽，確保手機重啟後鬧鐘能重新設置。
 * 3.適配 Android 12+ 精確鬧鐘權限：針對 targetSdk 36 處理了 SCHEDULE_EXACT_ALARM 的權限邏輯，如果系統未授予精確鬧鐘權限，會自動降級使用 setAndAllowWhileIdle 以保證在省電模式下也能儘快更新。
 * 4.優化廣播接收器：現在小工具會同時響應午夜鬧鐘、系統時間修改、時區修改及開機完成事件。
 * */
class LunarWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_MIDNIGHT_UPDATE = "com.example.myTools.action.MIDNIGHT_UPDATE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        // 當接收到這些廣播時，主動觸發小工具的更新
        if (action == ACTION_MIDNIGHT_UPDATE ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||  // 監聽系統時間手動變更
            action == Intent.ACTION_DATE_CHANGED ||  // 系統日期變更
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, LunarWidgetProvider::class.java)
            )
            
            // 觸發更新
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        // 每次更新時重新計算鬧鐘，確保即使因系統重啟等原因失效也能恢復
        scheduleMidnightUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleMidnightUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelMidnightUpdate(context)
    }

    private fun scheduleMidnightUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, LunarWidgetProvider::class.java).apply {
            action = ACTION_MIDNIGHT_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 計算下一個 0 點的時間
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 針對 Android 12+ (API 31) 的精確鬧鐘權限處理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (_: SecurityException) {
                    // 如果權限被取消，退而求其次使用非精確鬧鐘
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            // API 31 以下直接使用（這裏其實 minSdk 是 31，但保留邏輯完整性）
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun cancelMidnightUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, LunarWidgetProvider::class.java).apply {
            action = ACTION_MIDNIGHT_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.lunar_widget)

        // 獲取當前農曆信息
        val lunar = Lunar.fromDate(Date())
        val lunarDateStr = "${lunar.monthInChinese}月${lunar.dayInChinese}"
        
        // 獲取節氣
        val prevJieQi = lunar.getPrevJieQi(false)
        
        // 顯示當前的節氣名稱
        val solarTermStr = "節氣：${prevJieQi.name}"

        views.setTextViewText(R.id.tv_lunar_date, lunarDateStr)
        views.setTextViewText(R.id.tv_solar_term, solarTermStr)

        // 點擊事件：進入 app 的黃曆頁面
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_page", 0) // 0 是黃曆頁面
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
