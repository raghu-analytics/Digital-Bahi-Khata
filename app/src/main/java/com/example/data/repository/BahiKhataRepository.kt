package com.example.data.repository

import com.example.data.dao.AppSettingDao
import com.example.data.dao.CustomerDao
import com.example.data.dao.TransactionDao
import com.example.data.model.AppSetting
import com.example.data.model.Customer
import com.example.data.model.CustomerMonthlyBreakdown
import com.example.data.model.CustomerWithSummary
import com.example.data.model.DailySummary
import com.example.data.model.MonthlySummary
import com.example.data.model.Transaction
import com.example.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

class BahiKhataRepository(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    private val appSettingDao: AppSettingDao
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allSettings: Flow<List<AppSetting>> = appSettingDao.getAllSettings()

    fun getCustomerById(id: Long): Flow<Customer?> = customerDao.getCustomerById(id)

    suspend fun getCustomerByIdDirect(id: Long): Customer? = customerDao.getCustomerByIdDirect(id)

    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsForCustomer(customerId)

    fun getTransactionsForDate(date: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsForDate(date)

    fun getTransactionsInMonth(yearMonth: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsInMonth(yearMonth)

    fun getSettingValue(key: String): Flow<String?> = appSettingDao.getSettingValue(key)

    /**
     * Generates reactive Customers list with serial numbers and selected date's Udhar & Jama values.
     */
    fun getCustomersWithSummary(selectedDate: String): Flow<List<CustomerWithSummary>> {
        return combine(
            customerDao.getAllCustomers(),
            transactionDao.getAllTransactions()
        ) { rawCustomers, transactions ->
            // Sort by id / creation time to give consistent serial numbers (1), (2), (3)...
            val orderedCustomers = rawCustomers.sortedBy { it.id }
            val txnsByCustomer = transactions.groupBy { it.customerId }

            orderedCustomers.mapIndexed { index, customer ->
                val customerTxns = txnsByCustomer[customer.id] ?: emptyList()
                val selectedDateTxns = customerTxns.filter { it.entryDate == selectedDate }

                val dateCredit = selectedDateTxns.filter { it.isCredit }.sumOf { it.amountPaise }
                val datePayment = selectedDateTxns.filter { it.isPayment }.sumOf { it.amountPaise }

                val totalCredit = customerTxns.filter { it.isCredit }.sumOf { it.amountPaise }
                val totalPayment = customerTxns.filter { it.isPayment }.sumOf { it.amountPaise }
                val lastDate = customerTxns.maxByOrNull { it.entryDate }?.entryDate

                CustomerWithSummary(
                    customer = customer,
                    serialNumber = index + 1,
                    selectedDateCreditPaise = dateCredit,
                    selectedDatePaymentPaise = datePayment,
                    totalCreditPaise = totalCredit,
                    totalPaymentPaise = totalPayment,
                    activeOnSelectedDate = selectedDateTxns.isNotEmpty(),
                    lastTransactionDate = lastDate,
                    transactionCount = customerTxns.size
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Calculates the 4 dashboard summary metrics for a given date.
     */
    fun getDailySummary(selectedDate: String): Flow<DailySummary> {
        return combine(
            transactionDao.getAllTransactions(),
            customerDao.getAllCustomers()
        ) { transactions, _ ->
            val dateTransactions = transactions.filter { it.entryDate == selectedDate }
            val todayCredit = dateTransactions.filter { it.isCredit }.sumOf { it.amountPaise }
            val todayPayment = dateTransactions.filter { it.isPayment }.sumOf { it.amountPaise }
            val activeCount = dateTransactions.map { it.customerId }.distinct().size

            val allCredit = transactions.filter { it.isCredit }.sumOf { it.amountPaise }
            val allPayment = transactions.filter { it.isPayment }.sumOf { it.amountPaise }
            val totalBakaya = allCredit - allPayment

            DailySummary(
                selectedDate = selectedDate,
                todayCreditPaise = todayCredit,
                todayPaymentPaise = todayPayment,
                totalBakayaPaise = totalBakaya,
                activeCustomersCount = activeCount
            )
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Calculates monthly summary metrics for the given month ("YYYY-MM").
     */
    fun getMonthlySummary(yearMonth: String): Flow<MonthlySummary> {
        return transactionDao.getTransactionsInMonth(yearMonth).map { monthTransactions ->
            val totalCredit = monthTransactions.filter { it.isCredit }.sumOf { it.amountPaise }
            val totalPayment = monthTransactions.filter { it.isPayment }.sumOf { it.amountPaise }
            val uniqueDays = monthTransactions.map { it.entryDate }.distinct().size

            MonthlySummary(
                monthYear = yearMonth,
                totalCreditPaise = totalCredit,
                totalPaymentPaise = totalPayment,
                transactionDaysCount = uniqueDays,
                totalTransactionsCount = monthTransactions.size
            )
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Returns breakdown of each customer who had transactions in the specified month.
     */
    fun getMonthlyCustomerBreakdowns(yearMonth: String): Flow<List<CustomerMonthlyBreakdown>> {
        return combine(
            customerDao.getAllCustomers(),
            transactionDao.getAllTransactions()
        ) { rawCustomers, allTransactions ->
            val orderedCustomers = rawCustomers.sortedBy { it.id }
            val monthTxns = allTransactions.filter { it.entryDate.startsWith(yearMonth) }
            val monthTxnsByCustomer = monthTxns.groupBy { it.customerId }
            val allTxnsByCustomer = allTransactions.groupBy { it.customerId }

            orderedCustomers.mapIndexedNotNull { index, customer ->
                val mTxns = monthTxnsByCustomer[customer.id] ?: emptyList()
                val allCustTxns = allTxnsByCustomer[customer.id] ?: emptyList()

                val mCredit = mTxns.filter { it.isCredit }.sumOf { it.amountPaise }
                val mPayment = mTxns.filter { it.isPayment }.sumOf { it.amountPaise }
                val netMonthly = mCredit - mPayment

                val txnsUpToMonth = allCustTxns.filter { it.entryDate.take(7) <= yearMonth }
                val totalDebitUpToMonth = txnsUpToMonth.filter { it.isCredit }.sumOf { it.amountPaise }
                val totalCreditUpToMonth = txnsUpToMonth.filter { it.isPayment }.sumOf { it.amountPaise }
                val netBalanceUpToMonth = totalDebitUpToMonth - totalCreditUpToMonth

                val pichlaBakaya = netBalanceUpToMonth - mCredit
                val prabhaviUdhar = mCredit + pichlaBakaya

                val totalCustCredit = allCustTxns.filter { it.isCredit }.sumOf { it.amountPaise }
                val totalCustPayment = allCustTxns.filter { it.isPayment }.sumOf { it.amountPaise }
                val currentBakaya = totalCustCredit - totalCustPayment

                if (mTxns.isEmpty() && prabhaviUdhar == 0L && currentBakaya == 0L) {
                    null // No activity in month and no balance
                } else {
                    CustomerMonthlyBreakdown(
                        customer = customer,
                        serialNumber = index + 1,
                        monthlyCreditPaise = mCredit,
                        monthlyPaymentPaise = mPayment,
                        netMonthlyBalancePaise = netMonthly,
                        pichlaBakayaPaise = pichlaBakaya,
                        prabhaviUdharPaise = prabhaviUdhar,
                        currentTotalBakayaPaise = currentBakaya,
                        transactionCountInMonth = mTxns.size
                    )
                }
            }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun getMonthlyCustomerBreakdownsDirect(yearMonth: String): List<CustomerMonthlyBreakdown> = withContext(Dispatchers.IO) {
        val customers = customerDao.getAllCustomersDirect().sortedBy { it.id }
        val allTransactions = transactionDao.getAllTransactionsDirect()
        val monthTxns = allTransactions.filter { it.entryDate.startsWith(yearMonth) }
        val monthTxnsByCustomer = monthTxns.groupBy { it.customerId }
        val allTxnsByCustomer = allTransactions.groupBy { it.customerId }

        customers.mapIndexedNotNull { index, customer ->
            val mTxns = monthTxnsByCustomer[customer.id] ?: emptyList()
            val allCustTxns = allTxnsByCustomer[customer.id] ?: emptyList()

            val mCredit = mTxns.filter { it.isCredit }.sumOf { it.amountPaise }
            val mPayment = mTxns.filter { it.isPayment }.sumOf { it.amountPaise }
            val netMonthly = mCredit - mPayment

            val txnsUpToMonth = allCustTxns.filter { it.entryDate.take(7) <= yearMonth }
            val totalDebitUpToMonth = txnsUpToMonth.filter { it.isCredit }.sumOf { it.amountPaise }
            val totalCreditUpToMonth = txnsUpToMonth.filter { it.isPayment }.sumOf { it.amountPaise }
            val netBalanceUpToMonth = totalDebitUpToMonth - totalCreditUpToMonth

            val pichlaBakaya = netBalanceUpToMonth - mCredit
            val prabhaviUdhar = mCredit + pichlaBakaya

            val totalCustCredit = allCustTxns.filter { it.isCredit }.sumOf { it.amountPaise }
            val totalCustPayment = allCustTxns.filter { it.isPayment }.sumOf { it.amountPaise }
            val currentBakaya = totalCustCredit - totalCustPayment

            if (mTxns.isEmpty() && prabhaviUdhar == 0L && currentBakaya == 0L) {
                null
            } else {
                CustomerMonthlyBreakdown(
                    customer = customer,
                    serialNumber = index + 1,
                    monthlyCreditPaise = mCredit,
                    monthlyPaymentPaise = mPayment,
                    netMonthlyBalancePaise = netMonthly,
                    pichlaBakayaPaise = pichlaBakaya,
                    prabhaviUdharPaise = prabhaviUdhar,
                    currentTotalBakayaPaise = currentBakaya,
                    transactionCountInMonth = mTxns.size
                )
            }
        }
    }

    /**
     * Auto-generates unique sequential customer code e.g. "CUS-0001", "CUS-0002"
     */
    suspend fun generateNextCustomerCode(): String = withContext(Dispatchers.IO) {
        val latestCode = customerDao.getLatestCustomerCode()
        if (latestCode == null) {
            "CUS-0001"
        } else {
            val numPart = latestCode.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            String.format(Locale.US, "CUS-%04d", numPart + 1)
        }
    }

    suspend fun insertCustomer(
        name: String,
        mobile: String?,
        address: String?
    ): Customer = withContext(Dispatchers.IO) {
        val code = generateNextCustomerCode()
        val now = DateTimeUtils.getCurrentTimestampDb()
        val customer = Customer(
            customerCode = code,
            name = name.trim(),
            mobileNumber = mobile?.trim()?.ifBlank { null },
            address = address?.trim()?.ifBlank { null },
            createdAt = now,
            updatedAt = now
        )
        val id = customerDao.insertCustomer(customer)
        customer.copy(id = id)
    }

    suspend fun updateCustomer(customer: Customer): Int = withContext(Dispatchers.IO) {
        val updated = customer.copy(updatedAt = DateTimeUtils.getCurrentTimestampDb())
        customerDao.updateCustomer(updated)
    }

    suspend fun deleteCustomer(customer: Customer): Int = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun insertTransaction(
        customerId: Long,
        type: String,
        amountPaise: Long,
        description: String?,
        entryDate: String = DateTimeUtils.getTodayDateDb(),
        entryTime: String = DateTimeUtils.getCurrentTimeDb()
    ): Long = withContext(Dispatchers.IO) {
        val now = DateTimeUtils.getCurrentTimestampDb()
        val txn = Transaction(
            customerId = customerId,
            transactionType = type,
            amountPaise = amountPaise,
            description = description?.trim()?.ifBlank { null },
            entryDate = entryDate,
            entryTime = entryTime,
            createdAt = now,
            updatedAt = now
        )
        transactionDao.insertTransaction(txn)
    }

    suspend fun deleteTransaction(transaction: Transaction): Int = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        appSettingDao.saveSetting(AppSetting(key, value))
    }

    suspend fun saveSettings(settings: Map<String, String>) = withContext(Dispatchers.IO) {
        val list = settings.map { AppSetting(it.key, it.value) }
        appSettingDao.saveSettings(list)
    }

    suspend fun getAllCustomersDirect(): List<Customer> = customerDao.getAllCustomersDirect()
    suspend fun getAllTransactionsDirect(): List<Transaction> = transactionDao.getAllTransactionsDirect()
    suspend fun getAllSettingsDirect(): List<AppSetting> = appSettingDao.getAllSettingsDirect()

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactions()
        try { transactionDao.resetTransactionSequence() } catch (_: Exception) {}
        customerDao.deleteAllCustomers()
        try { customerDao.resetCustomerSequence() } catch (_: Exception) {}
    }
}
