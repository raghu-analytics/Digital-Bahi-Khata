package com.example.data.model

data class CustomerWithSummary(
    val customer: Customer,
    val serialNumber: Int = 1,
    val selectedDateCreditPaise: Long = 0L,
    val selectedDatePaymentPaise: Long = 0L,
    val totalCreditPaise: Long = 0L,
    val totalPaymentPaise: Long = 0L,
    val activeOnSelectedDate: Boolean = false,
    val lastTransactionDate: String? = null,
    val transactionCount: Int = 0
) {
    // Net Bakaya = Total Udhar - Total Jama. Positive means customer owes shop.
    val netBalancePaise: Long
        get() = totalCreditPaise - totalPaymentPaise

    val netSelectedDateBalancePaise: Long
        get() = selectedDateCreditPaise - selectedDatePaymentPaise

    val hasDue: Boolean
        get() = netBalancePaise > 0L

    val hasAdvance: Boolean
        get() = netBalancePaise < 0L

    val activeToday: Boolean
        get() = activeOnSelectedDate

    /**
     * Calculates a relevance score for a search query.
     * Returns null if the item does NOT match the query.
     * Lower score = higher priority / better match:
     * - Score 0: Exact full name match
     * - Score 1: Name starts with query (First letter / prefix match, e.g. "Suresh" for 'S')
     * - Score 2: A word inside name starts with query (e.g. "Kumar Sharma" for 'Sh')
     * - Score 10 + index: Query appears inside name at index position (earlier position appears first)
     * - Score 50: Serial number match
     * - Score 60+: Customer code match
     */
    fun getSearchRelevanceScore(query: String): Int? {
        val q = query.trim().lowercase()
        if (q.isBlank()) return 0

        val nameLower = customer.name.lowercase()

        // 1. Exact Name match -> Priority 0
        if (nameLower == q) return 0

        // 2. Name starts with query (First letter / prefix match) -> Priority 1
        if (nameLower.startsWith(q)) return 1

        // 3. Word within name starts with query (e.g. "Ramesh Sharma" with "Sh") -> Priority 2
        val words = nameLower.split(' ', '-', '.', '_')
        if (words.any { it.startsWith(q) }) return 2

        // 4. Substring in Name -> Priority 10 + index of match in name
        // (Earlier position in name comes first: e.g. index 1 before index 4)
        val nameIdx = nameLower.indexOf(q)
        if (nameIdx >= 0) {
            return 10 + nameIdx
        }

        // 5. Serial Number match: e.g. "1", "12", "(1)"
        val digitsOnly = q.filter { it.isDigit() }
        if (serialNumber.toString() == q) return 50
        if (digitsOnly.isNotEmpty() && serialNumber.toString() == digitsOnly) {
            if (q.all { it.isDigit() || it == '(' || it == ')' || it == '#' || it == ' ' }) {
                return 50
            }
        }

        // 6. Customer code match (strict)
        val codeLower = customer.customerCode.lowercase()
        if (codeLower == q) return 60
        if (q.startsWith("cus-") && q.length > 4 && codeLower.contains(q)) {
            return 70 + codeLower.indexOf(q)
        }
        if (digitsOnly.isNotEmpty() && (q.startsWith("cus") || q.startsWith("c-"))) {
            val codeNumeric = codeLower.filter { it.isDigit() }
            if (codeNumeric.contains(digitsOnly) || codeNumeric.trimStart('0') == digitsOnly) {
                return 80
            }
        }

        return null
    }

    /**
     * Accurately matches a search query against Customer Name, Serial Number, or Customer Code.
     * Prevents common prefix letters ('C', 'U', 'S', "CUS", "CUS-") from erroneously matching all customers.
     * Note: Phone number matching is disabled.
     */
    fun matchesSearchQuery(query: String): Boolean {
        return getSearchRelevanceScore(query) != null
    }
}

data class DailySummary(
    val selectedDate: String,
    val todayCreditPaise: Long = 0L,
    val todayPaymentPaise: Long = 0L,
    val totalBakayaPaise: Long = 0L,
    val activeCustomersCount: Int = 0
)

data class MonthlySummary(
    val monthYear: String, // "YYYY-MM"
    val totalCreditPaise: Long = 0L,
    val totalPaymentPaise: Long = 0L,
    val transactionDaysCount: Int = 0,
    val totalTransactionsCount: Int = 0
) {
    val netChangePaise: Long
        get() = totalCreditPaise - totalPaymentPaise

    val netDifferencePaise: Long
        get() = totalCreditPaise - totalPaymentPaise
}

data class CustomerMonthlyBreakdown(
    val customer: Customer,
    val serialNumber: Int = 1,
    val monthlyCreditPaise: Long,
    val monthlyPaymentPaise: Long,
    val netMonthlyBalancePaise: Long,
    val pichlaBakayaPaise: Long = 0L,
    val prabhaviUdharPaise: Long = 0L,
    val currentTotalBakayaPaise: Long,
    val transactionCountInMonth: Int
)
