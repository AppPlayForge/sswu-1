package com.example.myTools.bazi

import com.nlf.calendar.EightChar
import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import com.nlf.calendar.util.LunarUtil

/**
 * 格式化八字命盤資訊為文字，供複製到剪貼簿
 */
fun formatBaZiInfo(record: BaZiRecord): String {
    return try {
        val lunar = if (record.isLunar) {
            Lunar.fromYmdHms(record.year, record.month, record.day, record.hour, record.minute, 0)
        } else {
            Solar.fromYmdHms(record.year, record.month, record.day, record.hour, record.minute, 0).lunar
        }
        val solar = lunar.solar
        val solarFullStr = "${solar.year}-%02d-%02d %02d:%02d".format(
            solar.month,
            solar.day,
            solar.hour,
            solar.minute
        )
        val baZi = lunar.eightChar

        val yearGan = baZi.yearGan
        val yearGanWuXing = LunarUtil.WU_XING_GAN[yearGan] ?: ""
        val shengXiao = lunar.yearShengXiaoExact
        val ganShengXiaoStr = if (yearGanWuXing.isNotEmpty()) "$yearGanWuXing$shengXiao" else shengXiao

        val yearNaYin = baZi.yearNaYin
        val naYinWuXingChar = yearNaYin.lastOrNull()?.toString() ?: ""
        val naYinWuXingStr = if (naYinWuXingChar.isNotEmpty()) "$yearNaYin (${naYinWuXingChar}命)" else yearNaYin

        val dayGan = baZi.dayGan
        val dayGanWuXing = LunarUtil.WU_XING_GAN[dayGan] ?: ""
        val dayGanStr = "$dayGan${dayGanWuXing}日主"
        val placeStr = if (record.province.isEmpty() && record.city.isEmpty()) "未填寫" else "${record.province} ${record.city}"

        """
            【緣主命盤資訊】
            姓名：${record.name}
            性別：${record.gender}
            出生地：$placeStr
            公曆出生：$solarFullStr
            農曆出生：$lunar
            納音五行：$naYinWuXingStr
            生肖五行：$ganShengXiaoStr
            日幹屬性：$dayGanStr
            
            八字四柱：
            年柱：${baZi.year} (${baZi.yearShiShenGan}, 納音: ${baZi.yearNaYin})
            月柱：${baZi.month} (${baZi.monthShiShenGan}, 納音: ${baZi.monthNaYin})
            日柱：${baZi.day} (日主, 納音: ${baZi.dayNaYin})
            時柱：${baZi.time} (${baZi.timeShiShenGan}, 納音: ${baZi.timeNaYin})
            
            五行分布：${baZi.yearWuXing}${baZi.monthWuXing}${baZi.dayWuXing}${baZi.timeWuXing}
        """.trimIndent()
    } catch (_: Exception) {
        "姓名：${record.name}\n性別：${record.gender}\n出生：${record.year}-${record.month}-${record.day} ${record.hour}:${record.minute}"
    }
}

/**
 * 計算五行個數分佈
 */
fun calculateWuXingBalance(baZi: EightChar): String {
    val all = baZi.yearWuXing + baZi.monthWuXing + baZi.dayWuXing + baZi.timeWuXing
    val counts = mutableMapOf('金' to 0, '木' to 0, '水' to 0, '火' to 0, '土' to 0)
    all.forEach { char ->
        if (counts.containsKey(char)) {
            counts[char] = counts[char]!! + 1
        }
    }
    return counts.entries.joinToString("  ") { "${it.key}: ${it.value}" }
}
