package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AppSetting
import com.example.data.model.Customer
import com.example.data.model.CustomerMonthlyBreakdown
import com.example.data.model.CustomerWithSummary
import com.example.data.model.DailySummary
import com.example.data.model.MonthlySummary
import com.example.data.model.Transaction
import com.example.data.repository.BahiKhataRepository
import com.example.util.BackupExportUtils
import com.example.util.CalculatorEngine
import com.example.util.CurrencyUtils
import com.example.util.DateTimeUtils
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader

enum class CustomerSortOption(val titleHindi: String) {
    SERIAL_NO("क्रम संख्या (1, 2, 3..)"),
    NAME_ASC("नाम (A to Z)"),
    DAILY_UDHAR("आज का उधार"),
    DAILY_JAMA("आज का जमा")
}

enum class MonthlyBreakdownSort(val titleHindi: String) {
    SERIAL_NO("क्रम संख्या (1, 2, 3..)"),
    CREDIT_HIGH("अधिक उधार पहले"),
    PAYMENT_HIGH("अधिक जमा पहले"),
    BALANCE_HIGH("अधिक बकाया पहले"),
    ALPHABETICAL("नाम अनुसार")
}

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class PdfGenerated(val file: File, val title: String) : UiEvent()
    data class ExcelGenerated(val file: File) : UiEvent()
    data class BackupCompleted(val file: File) : UiEvent()
    data class RestoreCompleted(val success: Boolean) : UiEvent()
    data object RestartApp : UiEvent()
}

class BahiKhataViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = BahiKhataRepository(
        db.customerDao(),
        db.transactionDao(),
        db.appSettingDao()
    )

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    // Holds validation details for the database selected for restore.
    // The actual restore starts only after the user confirms these details.
    private val _restoreValidationMessage = MutableStateFlow<String?>(null)
    val restoreValidationMessage: StateFlow<String?> = _restoreValidationMessage.asStateFlow()

    private var pendingRestoreUri: Uri? = null

    // --- HOME DASHBOARD STATE ---
    private val _selectedDashboardDate = MutableStateFlow(DateTimeUtils.getTodayDateDb())
    val selectedDashboardDate: StateFlow<String> = _selectedDashboardDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(CustomerSortOption.SERIAL_NO)
    val sortOption: StateFlow<CustomerSortOption> = _sortOption.asStateFlow()

    val dailySummary: StateFlow<DailySummary> = _selectedDashboardDate
        .flatMapLatest { date -> repository.getDailySummary(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailySummary(DateTimeUtils.getTodayDateDb()))

    val allCustomersWithSummary: StateFlow<List<CustomerWithSummary>> = _selectedDashboardDate
        .flatMapLatest { date -> repository.getCustomersWithSummary(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCustomers: StateFlow<List<CustomerWithSummary>> = combine(
        allCustomersWithSummary,
        _searchQuery,
        _sortOption
    ) { list, query, sort ->
        if (query.isBlank()) {
            when (sort) {
                CustomerSortOption.SERIAL_NO -> list.sortedBy { it.serialNumber }
                CustomerSortOption.NAME_ASC -> list.sortedBy { it.customer.name.lowercase() }
                CustomerSortOption.DAILY_UDHAR -> list.sortedByDescending { it.selectedDateCreditPaise }
                CustomerSortOption.DAILY_JAMA -> list.sortedByDescending { it.selectedDatePaymentPaise }
            }
        } else {
            val matchedWithScore = list.mapNotNull { item ->
                val score = item.getSearchRelevanceScore(query)
                if (score != null) item to score else null
            }
            // Sort by relevance score first (starts with query first, earlier letter positions next),
            // then by name alphabetically, then by serial number
            matchedWithScore.sortedWith(
                compareBy<Pair<CustomerWithSummary, Int>> { it.second }
                    .thenBy { it.first.customer.name.lowercase() }
                    .thenBy { it.first.serialNumber }
            ).map { it.first }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCustomersWithSummary: StateFlow<List<CustomerWithSummary>> = filteredCustomers

    // All raw customers for customer selector in entry
    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- MONTHLY REPORT STATE ---
    private val _selectedMonth = MutableStateFlow(DateTimeUtils.getCurrentMonthDb())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _monthlyBreakdownSort = MutableStateFlow(MonthlyBreakdownSort.SERIAL_NO)
    val monthlyBreakdownSort: StateFlow<MonthlyBreakdownSort> = _monthlyBreakdownSort.asStateFlow()

    val monthlySummary: StateFlow<MonthlySummary> = _selectedMonth
        .flatMapLatest { month -> repository.getMonthlySummary(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlySummary(DateTimeUtils.getCurrentMonthDb()))

    val monthlyCustomerBreakdowns: StateFlow<List<CustomerMonthlyBreakdown>> = combine(
        _selectedMonth.flatMapLatest { month -> repository.getMonthlyCustomerBreakdowns(month) },
        _monthlyBreakdownSort
    ) { list, sort ->
        when (sort) {
            MonthlyBreakdownSort.SERIAL_NO -> list.sortedBy { it.serialNumber }
            MonthlyBreakdownSort.CREDIT_HIGH -> list.sortedByDescending { it.monthlyCreditPaise }
            MonthlyBreakdownSort.PAYMENT_HIGH -> list.sortedByDescending { it.monthlyPaymentPaise }
            MonthlyBreakdownSort.BALANCE_HIGH -> list.sortedByDescending { it.prabhaviUdharPaise }
            MonthlyBreakdownSort.ALPHABETICAL -> list.sortedBy { it.customer.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SETTINGS STATE ---
    val shopSettings: StateFlow<Map<String, String>> = repository.allSettings.map { list ->
        list.associate { it.key to it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Actions: Home & Dashboard ---
    fun setSelectedDashboardDate(date: String) {
        _selectedDashboardDate.value = date
    }

    fun stepDashboardDate(offset: Int) {
        _selectedDashboardDate.value = DateTimeUtils.getAdjacentDate(_selectedDashboardDate.value, offset)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: CustomerSortOption) {
        _sortOption.value = option
    }

    fun addNewCustomer(name: String, mobile: String?, address: String?, onAdded: ((Customer) -> Unit)? = null) {
        if (name.isBlank()) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowToast("कृपया ग्राहक का नाम दर्ज करें!")) }
            return
        }
        viewModelScope.launch {
            try {
                val customer = repository.insertCustomer(name, mobile, address)
                _eventFlow.emit(UiEvent.ShowToast("ग्राहक '${customer.name}' सफलतापूर्वक जोड़ा गया!"))
                onAdded?.invoke(customer)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("ग्राहक जोड़ने में त्रुटि: ${e.localizedMessage}"))
            }
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            try {
                repository.updateCustomer(customer)
                _eventFlow.emit(UiEvent.ShowToast("ग्राहक विवरण अपडेट हो गया!"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("अपडेट विफल: ${e.localizedMessage}"))
            }
        }
    }

    fun deleteCustomer(customer: Customer, onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customer)
                _eventFlow.emit(UiEvent.ShowToast("ग्राहक '${customer.name}' और उनका खाता हटा दिया गया!"))
                onDeleted()
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("हटाने में त्रुटि: ${e.localizedMessage}"))
            }
        }
    }

    // --- Customer Detail Data Providers ---
    fun getCustomerFromCache(id: Long): Customer? {
        return filteredCustomers.value.find { it.customer.id == id }?.customer
            ?: allCustomers.value.find { it.id == id }
    }

    fun getCustomerById(id: Long): Flow<Customer?> {
        return repository.getCustomerById(id)
    }

    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>> {
        return repository.getTransactionsForCustomer(customerId)
    }

    // --- Actions: Transactions & Calculator ---
    fun addTransaction(
        customerId: Long,
        type: String,
        amountInput: String,
        description: String?,
        entryDate: String = DateTimeUtils.getTodayDateDb(),
        entryTime: String = DateTimeUtils.getCurrentTimeDb(),
        onSuccess: (Transaction) -> Unit
    ) {
        // First evaluate if it's an expression
        val evaluatedAmount = CalculatorEngine.evaluate(amountInput) ?: amountInput
        val amountPaise = CurrencyUtils.rupeesStringToPaise(evaluatedAmount)

        if (amountPaise <= 0L) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowToast("कृपया वैध राशि (शून्य से अधिक) दर्ज करें!")) }
            return
        }

        viewModelScope.launch {
            try {
                val txnId = repository.insertTransaction(
                    customerId = customerId,
                    type = type,
                    amountPaise = amountPaise,
                    description = description,
                    entryDate = entryDate,
                    entryTime = entryTime
                )
                val txn = Transaction(
                    id = txnId,
                    customerId = customerId,
                    transactionType = type,
                    amountPaise = amountPaise,
                    description = description,
                    entryDate = entryDate,
                    entryTime = entryTime,
                    createdAt = DateTimeUtils.getCurrentTimestampDb(),
                    updatedAt = DateTimeUtils.getCurrentTimestampDb()
                )
                val typeName = if (type == Transaction.TYPE_CREDIT) "उधार" else "जमा"
                _eventFlow.emit(UiEvent.ShowToast("$typeName ₹${CurrencyUtils.formatPaiseToRupees(amountPaise, false)} सफलतापूर्वक दर्ज हुआ!"))
                onSuccess(txn)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("लेन-देन दर्ज करने में विफल: ${e.localizedMessage}"))
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
                _eventFlow.emit(UiEvent.ShowToast("लेन-देन प्रविष्टि हटा दी गई!"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("हटाने में त्रुटि: ${e.localizedMessage}"))
            }
        }
    }

    // --- Actions: Monthly Report ---
    fun setSelectedMonth(monthYear: String) {
        _selectedMonth.value = monthYear
    }

    fun setMonthlyBreakdownSort(sort: MonthlyBreakdownSort) {
        _monthlyBreakdownSort.value = sort
    }

    // --- Actions: PDF Exports ---
    fun exportCustomerLedgerPdf(customer: Customer, monthFilter: String? = null) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val allTxns = repository.getAllTransactionsDirect().filter { it.customerId == customer.id }
                val settings = shopSettings.value
                val pdfFile = withContext(Dispatchers.IO) {
                    PdfReportGenerator.generateCustomerLedgerPdf(
                        context = context,
                        shopSettings = settings,
                        customer = customer,
                        transactions = allTxns,
                        monthFilter = monthFilter
                    )
                }
                if (pdfFile != null) {
                    _eventFlow.emit(UiEvent.PdfGenerated(pdfFile, "खाता पर्ची - ${customer.name}"))
                } else {
                    _eventFlow.emit(UiEvent.ShowToast("PDF बनाने में त्रुटि हुई!"))
                }
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("PDF त्रुटि: ${e.localizedMessage}"))
            }
        }
    }

    fun exportMonthlyTransactionsPdf(monthYear: String) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val breakdowns = repository.getMonthlyCustomerBreakdownsDirect(monthYear)
                val settings = shopSettings.value
                val pdfFile = withContext(Dispatchers.IO) {
                    PdfReportGenerator.generateMonthlyTransactionsPdf(
                        context = context,
                        shopSettings = settings,
                        monthYear = monthYear,
                        breakdowns = breakdowns
                    )
                }
                if (pdfFile != null) {
                    _eventFlow.emit(UiEvent.PdfGenerated(pdfFile, "मासिक ग्राहक सारांश रिपोर्ट - $monthYear"))
                } else {
                    _eventFlow.emit(UiEvent.ShowToast("PDF बनाने में त्रुटि हुई!"))
                }
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("PDF त्रुटि: ${e.localizedMessage}"))
            }
        }
    }

    fun exportMonthlyCustomerVouchersPdf(monthYear: String) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val breakdowns = monthlyCustomerBreakdowns.value
                val settings = shopSettings.value
                val pdfFile = withContext(Dispatchers.IO) {
                    PdfReportGenerator.generateMonthlyCustomerVouchersPdf(
                        context = context,
                        shopSettings = settings,
                        monthYear = monthYear,
                        breakdowns = breakdowns
                    )
                }
                if (pdfFile != null) {
                    _eventFlow.emit(UiEvent.PdfGenerated(pdfFile, "मासिक ग्राहक बिल वाउचर - $monthYear"))
                } else {
                    _eventFlow.emit(UiEvent.ShowToast("PDF बनाने में त्रुटि हुई!"))
                }
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("PDF त्रुटि: ${e.localizedMessage}"))
            }
        }
    }

    // --- Actions: Settings, Backup & Excel Export ---
    fun saveShopSettings(shopName: String, ownerName: String, address: String, mobile: String) {
        viewModelScope.launch {
            try {
                repository.saveSettings(
                    mapOf(
                        AppSetting.KEY_SHOP_NAME to shopName.trim(),
                        AppSetting.KEY_OWNER_NAME to ownerName.trim(),
                        AppSetting.KEY_ADDRESS to address.trim(),
                        AppSetting.KEY_MOBILE to mobile.trim()
                    )
                )
                _eventFlow.emit(UiEvent.ShowToast("दुकान का विवरण सुरक्षित हो गया!"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("सुरक्षित करने में त्रुटि: ${e.localizedMessage}"))
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val backupFile = BackupExportUtils.createDatabaseBackup(context)
                if (backupFile != null) {
                    _eventFlow.emit(UiEvent.BackupCompleted(backupFile))
                    _eventFlow.emit(UiEvent.ShowToast("file saved at /Downloads/digital-bahi-khata/${backupFile.name}"))
                } else {
                    _eventFlow.emit(UiEvent.ShowToast("बैकअप बनाने में विफलता!"))
                }
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("बैकअप त्रुटि: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Validates a selected database and shows its details to the user.
     * No database replacement happens here.
     */
    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()

                if (!BackupExportUtils.isDbFile(context, uri)) {
                    _eventFlow.emit(
                        UiEvent.ShowToast(
                            "अमान्य फ़ाइल! केवल .db या .sqlite बैकअप फ़ाइल चुनें।"
                        )
                    )
                    return@launch
                }

                val validationMessage = BackupExportUtils.validateDatabaseFile(context, uri)

                if (validationMessage != null && validationMessage.startsWith("Database is valid.")) {
                    pendingRestoreUri = uri
                    _restoreValidationMessage.value = validationMessage
                } else {
                    _eventFlow.emit(
                        UiEvent.ShowToast(
                            validationMessage ?: "डेटाबेस validation विफल रही।"
                        )
                    )
                }
            } catch (e: Exception) {
                _eventFlow.emit(
                    UiEvent.ShowToast(
                        "डेटाबेस validation त्रुटि: ${e.localizedMessage ?: "Unknown error"}"
                    )
                )
            }
        }
    }

    /**
     * Cancels the pending database restore preview.
     */
    fun cancelRestorePreview() {
        pendingRestoreUri = null
        _restoreValidationMessage.value = null
    }

    /**
     * Restores the database only after the user has confirmed the validation details.
     */
    fun confirmRestore() {
        val uri = pendingRestoreUri ?: return

        pendingRestoreUri = null
        _restoreValidationMessage.value = null

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val (success, message) = BackupExportUtils.restoreDatabaseBackup(context, uri)

                _eventFlow.emit(UiEvent.RestoreCompleted(success))

                if (success) {
                    _eventFlow.emit(
                        UiEvent.ShowToast(
                            "डेटाबेस सफलतापूर्वक Restore हो गया! ऐप रीस्टार्ट हो रहा है..."
                        )
                    )
                    _eventFlow.emit(UiEvent.RestartApp)
                } else {
                    _eventFlow.emit(UiEvent.ShowToast(message))
                }
            } catch (e: Exception) {
                _eventFlow.emit(
                    UiEvent.ShowToast(
                        "पुनर्स्थापना त्रुटि: ${e.localizedMessage ?: "Unknown error"}"
                    )
                )
            }
        }
    }

    fun exportToExcelCsv() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val customers = repository.getAllCustomersDirect()
                val transactions = repository.getAllTransactionsDirect()
                val settings = repository.getAllSettingsDirect()

                val file = BackupExportUtils.exportAllTablesToExcelCsv(context, customers, transactions, settings)
                if (file != null) {
                    _eventFlow.emit(UiEvent.ExcelGenerated(file))
                    _eventFlow.emit(UiEvent.ShowToast("file saved at /Downloads/digital-bahi-khata/${file.name}"))
                } else {
                    _eventFlow.emit(UiEvent.ShowToast("Excel निर्यात विफल रहा!"))
                }
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("निर्यात त्रुटि: ${e.localizedMessage}"))
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                _eventFlow.emit(UiEvent.ShowToast("सभी डेटा हटा दिया गया! ऐप रीस्टार्ट हो रहा है..."))
                _eventFlow.emit(UiEvent.RestartApp)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("डेटा साफ़ करने में त्रुटि: ${e.localizedMessage}"))
            }
        }
    }

    fun loadFaqContent(): String {
        return try {
            val context = getApplication<Application>()
            context.assets.open("faq_help_hindi.txt").use { input ->
                InputStreamReader(input, Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            }
        } catch (_: Exception) {
            "मदद एवं सहायता लोड नहीं हो सकी।"
        }
    }
}
