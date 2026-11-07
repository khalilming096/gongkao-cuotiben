package com.gongkao.cuotiben.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 记录时间展示：今天 / 昨天 / 具体日期 */
object TimeText {

    private val hm = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val md = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)
    private val ymd = SimpleDateFormat("yyyy/M/d HH:mm", Locale.CHINA)

    fun format(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }

        val sameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        val yesterday = now.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        return when {
            sameDay -> "今天 " + hm.format(date)
            isYesterday -> "昨天 " + hm.format(date)
            now.get(Calendar.YEAR) == target.get(Calendar.YEAR) -> md.format(date)
            else -> ymd.format(date)
        }
    }
}
