package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    private val DATE_FORMAT_DB = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val TIME_FORMAT_DB = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val TIMESTAMP_FORMAT_DB = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val MONTH_FORMAT_DB = SimpleDateFormat("yyyy-MM", Locale.US)

    private val HINDI_MONTHS = arrayOf(
        "जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून",
        "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
    )

    fun getTodayDateDb(): String {
        return DATE_FORMAT_DB.format(Date())
    }

    fun getCurrentTimeDb(): String {
        return TIME_FORMAT_DB.format(Date())
    }

    fun getCurrentTimestampDb(): String {
        return TIMESTAMP_FORMAT_DB.format(Date())
    }

    fun getCurrentMonthDb(): String {
        return MONTH_FORMAT_DB.format(Date())
    }

    /**
     * Converts "YYYY-MM-DD" to Hindi readable "15 अगस्त 2026"
     */
    fun formatToHindiDate(dateStr: String): String {
        return try {
            val date = DATE_FORMAT_DB.parse(dateStr) ?: return dateStr
            val cal = Calendar.getInstance().apply { time = date }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val monthIndex = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)
            val monthName = HINDI_MONTHS.getOrElse(monthIndex) { "" }

            val todayStr = getTodayDateDb()
            val calToday = Calendar.getInstance()
            calToday.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = DATE_FORMAT_DB.format(calToday.time)

            when (dateStr) {
                todayStr -> "आज, $day $monthName $year"
                yesterdayStr -> "कल, $day $monthName $year"
                else -> "$day $monthName $year"
            }
        } catch (_: Exception) {
            dateStr
        }
    }

    /**
     * Converts "YYYY-MM-DD" to short Hindi "15 अगस्त"
     */
    fun formatToShortHindiDate(dateStr: String): String {
        return try {
            val date = DATE_FORMAT_DB.parse(dateStr) ?: return dateStr
            val cal = Calendar.getInstance().apply { time = date }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val monthIndex = cal.get(Calendar.MONTH)
            val monthName = HINDI_MONTHS.getOrElse(monthIndex) { "" }
            "$day $monthName"
        } catch (_: Exception) {
            dateStr
        }
    }

    /**
     * Converts "HH:mm:ss" to 12-hour format e.g. "04:30 PM"
     */
    fun formatTo12HourTime(timeStr: String): String {
        return try {
            val time = TIME_FORMAT_DB.parse(timeStr) ?: return timeStr
            val format12 = SimpleDateFormat("hh:mm a", Locale.US)
            format12.format(time)
        } catch (_: Exception) {
            timeStr
        }
    }

    /**
     * Converts "YYYY-MM" to "अगस्त 2026"
     */
    fun formatMonthToHindi(monthYearStr: String): String {
        return try {
            val parts = monthYearStr.split("-")
            val year = parts[0]
            val monthInt = parts[1].toInt()
            val monthName = HINDI_MONTHS.getOrElse(monthInt - 1) { "" }
            "$monthName $year"
        } catch (_: Exception) {
            monthYearStr
        }
    }

    /**
     * Converts "YYYY-MM" to English "August 2026"
     */
    fun formatMonthToEnglish(monthYearStr: String): String {
        return try {
            val parts = monthYearStr.split("-")
            val year = parts[0]
            val monthInt = parts[1].toInt()
            val englishMonths = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            val monthName = englishMonths.getOrElse(monthInt - 1) { "" }
            "$monthName $year"
        } catch (_: Exception) {
            monthYearStr
        }
    }

    /**
     * Returns a list of past N months in "YYYY-MM" format with display Hindi labels
     */
    fun getPastMonths(count: Int = 12): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance()
        for (i in 0 until count) {
            val dbFormat = MONTH_FORMAT_DB.format(cal.time)
            val hindiLabel = formatMonthToHindi(dbFormat)
            list.add(Pair(dbFormat, hindiLabel))
            cal.add(Calendar.MONTH, -1)
        }
        return list
    }

    fun getAdjacentDate(dateStr: String, daysOffset: Int): String {
        return try {
            val date = DATE_FORMAT_DB.parse(dateStr) ?: Date()
            val cal = Calendar.getInstance().apply { time = date }
            cal.add(Calendar.DAY_OF_MONTH, daysOffset)
            val newDateStr = DATE_FORMAT_DB.format(cal.time)
            val todayStr = getTodayDateDb()
            if (daysOffset > 0 && newDateStr > todayStr) {
                todayStr
            } else {
                newDateStr
            }
        } catch (_: Exception) {
            getTodayDateDb()
        }
    }
}
