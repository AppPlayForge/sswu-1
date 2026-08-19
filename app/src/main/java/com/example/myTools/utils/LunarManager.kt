package com.example.myTools.utils

import com.nlf.calendar.Lunar
import java.util.Date

/**
 * 統一的農曆與節氣管理中心
 */
object LunarManager {

    data class LunarInfo(
        val lunarDate: String,
        val solarTerm: String,
        val shengXiao: String,
        val ganZhiYear: String,
        val monthInChinese: String,
        val dayInChinese: String
    )

    fun getTodayLunarInfo(): LunarInfo {
        val lunar = Lunar.fromDate(Date())
        
        // 獲取日期
        val lunarDate = "${lunar.monthInChinese}月${lunar.dayInChinese}"
        
        // 獲取節氣：優先檢查今天是否是節氣，如果不是，則顯示當前所處的節氣
        val currentJieQi = lunar.getJieQi()
        val solarTerm = if (currentJieQi.isNotEmpty()) {
            currentJieQi // 今天就是節氣
        } else {
            // 今天不是節氣，獲取最近的一個（包含今天在內的上一個節氣）
            lunar.getPrevJieQi(true).name
        }

        return LunarInfo(
            lunarDate = lunarDate,
            solarTerm = solarTerm,
            shengXiao = lunar.yearShengXiao,
            ganZhiYear = lunar.yearInGanZhi,
            monthInChinese = lunar.monthInChinese,
            dayInChinese = lunar.dayInChinese
        )
    }
}
