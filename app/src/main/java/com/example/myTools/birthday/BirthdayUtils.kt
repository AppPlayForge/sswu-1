package com.example.myTools.birthday

import com.nlf.calendar.Lunar
import java.util.Calendar

/**
 * 獲取農曆月份名稱 (如：正月（1月）、二月、冬月（11月）、臘月（12月）)
 */
fun getLunarMonthName(month: Int): String {
    val names = arrayOf(
        "正月（1月）", "二月", "三月", "四月", "五月", "六月", 
        "七月", "八月", "九月", "十月", "冬月（11月）", "臘月（12月）"
    )
    return if (month in 1..12) names[month - 1] else "${month}月"
}

/**
 * 獲取農曆日期名稱 (如：初一、廿一、三十)
 */
fun getLunarDayName(day: Int): String {
    // 這裡我們直接利用庫的內部邏輯或簡化邏輯，或者直接手動對應
    // 考慮到庫可能需要一個完整的 Lunar 對象，我們這裡手動映射最常用的名稱
    val names = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )
    return if (day in 1..30) names[day - 1] else "${day}日"
}

/**
 * 根據農曆月日計算下一個公曆生日的 Calendar
 */
fun getNextBirthdayCalendar(lunarMonth: Int, lunarDay: Int): Calendar {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayLunar = Lunar.fromDate(today.time)
    var nextBirthdayLunar = Lunar.fromYmd(todayLunar.year, lunarMonth, lunarDay)
    var nextBirthdaySolar = nextBirthdayLunar.solar
    val targetCalendar = Calendar.getInstance()
    targetCalendar.set(nextBirthdaySolar.year, nextBirthdaySolar.month - 1, nextBirthdaySolar.day, 0, 0, 0)
    targetCalendar.set(Calendar.MILLISECOND, 0)

    if (targetCalendar.timeInMillis < today.timeInMillis) {
        nextBirthdayLunar = Lunar.fromYmd(todayLunar.year + 1, lunarMonth, lunarDay)
        nextBirthdaySolar = nextBirthdayLunar.solar
        targetCalendar.set(nextBirthdaySolar.year, nextBirthdaySolar.month - 1, nextBirthdaySolar.day, 0, 0, 0)
        targetCalendar.set(Calendar.MILLISECOND, 0)
    }
    return targetCalendar
}
