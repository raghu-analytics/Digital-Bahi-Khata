package com.example.util

import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyUtils {

    /**
     * Convert Integer Paise (e.g. 150000 paise = 1500.00 rupees) to formatted Indian Rupee string.
     * e.g. "₹1,500.00" or "₹1,50,000.00"
     */
    fun formatPaiseToRupees(
        paise: Long,
        includeSymbol: Boolean = true,
        includeDecimal: Boolean = true
    ): String {
        val isNegative = paise < 0
        val positivePaise = abs(paise)
        val rupees = positivePaise / 100
        val remainingPaise = positivePaise % 100

        val formattedRupees = formatIndianNumberSystem(rupees)

        val result = if (includeDecimal) {
            val paiseFormatted = String.format(Locale.US, "%02d", remainingPaise)
            "$formattedRupees.$paiseFormatted"
        } else {
            formattedRupees
        }

        val prefix = if (isNegative) "- " else ""
        val symbol = if (includeSymbol) "₹" else ""

        return "$prefix$symbol$result"
    }

    /**
     * Formats integer amount according to Indian numbering system (e.g. 1,23,45,678)
     */
    private fun formatIndianNumberSystem(value: Long): String {
        if (value < 1000) return value.toString()

        val str = value.toString()
        val len = str.length
        val last3 = str.substring(len - 3)
        var remaining = str.substring(0, len - 3)

        val sb = StringBuilder()
        while (remaining.length > 2) {
            val chunk = remaining.substring(remaining.length - 2)
            sb.insert(0, ",$chunk")
            remaining = remaining.substring(0, remaining.length - 2)
        }
        if (remaining.isNotEmpty()) {
            sb.insert(0, remaining)
        }
        sb.append(",$last3")
        return sb.toString()
    }

    /**
     * Parses numeric input string (like "150.50" or "150") into Integer Paise (Long).
     */
    fun rupeesStringToPaise(input: String): Long {
        val sanitized = input.trim().replace(",", "").replace("₹", "")
        if (sanitized.isEmpty()) return 0L

        return try {
            if (sanitized.contains(".")) {
                val parts = sanitized.split(".")
                val rupeesPart = parts[0].toLongOrNull() ?: 0L
                val decimals = parts.getOrNull(1)?.take(2)?.padEnd(2, '0') ?: "00"
                val paisePart = decimals.take(2).toLongOrNull() ?: 0L
                (rupeesPart * 100L) + paisePart
            } else {
                (sanitized.toLongOrNull() ?: 0L) * 100L
            }
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Converts paise to Double Rupees for internal calculations if needed.
     */
    fun paiseToDouble(paise: Long): Double {
        return paise.toDouble() / 100.0
    }
}
