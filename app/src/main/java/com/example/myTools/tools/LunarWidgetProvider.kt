package com.example.myTools.tools

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.widget.RemoteViews
import com.example.myTools.MainActivity
import com.example.myTools.R
import com.nlf.calendar.Lunar
import java.util.Date


//桌面小部件
class LunarWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        //當接收到這些廣播時，會主動觸發小工具的更新
        if ((action == Intent.ACTION_TIME_CHANGED) ||  //使其能夠監聽系統的時間變更
            (action == Intent.ACTION_DATE_CHANGED) ||  //日期變更
            //時區變更
            (action == Intent.ACTION_TIMEZONE_CHANGED)) {
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, LunarWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.lunar_widget)

        // 獲取當前農曆信息
        val lunar = Lunar.fromDate(Date())
        val lunarDateStr = "${lunar.monthInChinese}月${lunar.dayInChinese}"
        
        // 獲取節氣
        val prevJieQi = lunar.getPrevJieQi(false)
        
        // 這裡我們顯示當前的節氣名稱
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
