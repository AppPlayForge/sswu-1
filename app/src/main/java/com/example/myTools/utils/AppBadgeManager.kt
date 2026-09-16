package com.example.myTools.utils

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.myTools.MainActivity
import com.example.myTools.birthday.BirthdayManager
import com.example.myTools.birthday.getDaysUntilBirthday
import com.example.myTools.carspeed.LocationData
import com.example.myTools.period.PeriodDataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全域紅點/數字提示狀態管理器 (AppBadgeManager)
 * 統一管理生日提醒、經期預測提醒、車速記錄狀態，以及動態綁定底欄/工具箱圖標紅點數字與手機通知。
 */
object AppBadgeManager {

    // 近 3 天內的生日提醒數量 (<= 3 天)
    private val _upcomingBirthdayCount = MutableStateFlow(0)
    val upcomingBirthdayCount: StateFlow<Int> = _upcomingBirthdayCount.asStateFlow()

    // 經期預測近 3 天內的提醒狀態 (0=無提醒, 1=預測即將來臨)
    private val _upcomingPeriodCount = MutableStateFlow(0)
    val upcomingPeriodCount: StateFlow<Int> = _upcomingPeriodCount.asStateFlow()

    // 車速儀記錄狀態 (代理 LocationData.isRecording)
    val isCarSpeedRecording: StateFlow<Boolean>
        get() = LocationData.isRecording

    /**
     * 檢查手機通知權限是否已開啟 (相容 Android 13+ 與舊版本)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.areNotificationsEnabled() ?: true
        }
    }

    /**
     * 刷新生日提醒數量 (距離生日 <= 3 天者)
     */
    fun refreshBirthdayBadges(context: Context) {
        val list = BirthdayManager.loadList(context)
        val count = list.count { record ->
            val days = getDaysUntilBirthday(record.lunarMonth, record.lunarDay)
            days in 0..3
        }
        _upcomingBirthdayCount.value = count
    }

    /**
     * 刷新經期預測提醒狀態 (距離預測經期 <= 3 天者)，並於需要時發送手機通知欄提醒
     */
    fun refreshPeriodBadges(context: Context) {
        val dataManager = PeriodDataManager(context)
        val records = dataManager.getRecords()
        val days = dataManager.getDaysUntilNextPeriod(records)

        if (days != null && days in 0..3) {
            _upcomingPeriodCount.value = 1
            sendPeriodNotificationIfNeeded(context, days)
        } else {
            _upcomingPeriodCount.value = 0
        }
    }

    /**
     * 當預測經期在 3 天內時，每日發送一次手機通知欄提示 (需先檢查通知權限)
     */
    private fun sendPeriodNotificationIfNeeded(context: Context, days: Int) {
        if (!hasNotificationPermission(context)) return

        val prefs = context.getSharedPreferences("period_notif_prefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastNotifDate = prefs.getString("last_notif_date", "")

        if (lastNotifDate != todayStr) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channelId = "period_reminder_channel"

            val channel = NotificationChannel(
                channelId,
                "月經週期提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "提供經期預測與生理週期提醒"
            }
            notificationManager.createNotificationChannel(channel)

            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("target_page", 3)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val msg = if (days == 0) {
                "預測您的月經將於今日來臨，請注意做好準備。"
            } else {
                "預測您的月經將於 $days 天後來臨，請提前做好準備。"
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_popup_reminder)
                .setContentTitle("月經週期提醒")
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(1003, notification)
            prefs.edit { putString("last_notif_date", todayStr) }
        }
    }

    /**
     * 計算特定導航路由 (route) 在底欄應顯示的數字提醒數量。
     * 若該功能未固定於底欄（存在於「工具箱」內），將自動累積傳遞給「工具箱」頁籤。
     */
    fun getBadgeCountForRoute(
        route: String,
        bottomBarSlots: List<String>,
        upcomingBirthdayCount: Int,
        isCarSpeedRecording: Boolean,
        upcomingPeriodCount: Int = 0
    ): Int {
        val isBirthdayPinned = bottomBarSlots.contains("birthday")
        val isCarSpeedPinned = bottomBarSlots.contains("carspeed")
        val isPeriodPinned = bottomBarSlots.contains("period")

        return when (route) {
            "birthday" -> upcomingBirthdayCount
            "carspeed" -> if (isCarSpeedRecording) 1 else 0
            "period" -> upcomingPeriodCount
            "tools" -> {
                var total = 0
                if (!isBirthdayPinned && upcomingBirthdayCount > 0) {
                    total += upcomingBirthdayCount
                }
                if (!isCarSpeedPinned && isCarSpeedRecording) {
                    total += 1
                }
                if (!isPeriodPinned && upcomingPeriodCount > 0) {
                    total += upcomingPeriodCount
                }
                total
            }
            else -> 0
        }
    }
}
