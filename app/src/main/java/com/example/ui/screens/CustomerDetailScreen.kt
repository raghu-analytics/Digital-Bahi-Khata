package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.ui.components.BahiKhataTopBar
import com.example.ui.components.TopBarLightPurpleAccent
import com.example.ui.components.TopBarLightPurpleBg
import com.example.ui.components.TopBarLightPurpleBorder
import com.example.ui.components.TopBarLightPurpleContent
import com.example.ui.theme.BorderLight
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.JamaGreenBg
import com.example.ui.theme.LedgerRedPrimary
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SaffronGoldContainer
import com.example.ui.theme.SaffronGoldDark
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.UdharRed
import com.example.ui.theme.UdharRedBg
import com.example.ui.theme.WarmCanvasBg
import com.example.ui.viewmodel.BahiKhataViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateTimeUtils

@Composable
fun CustomerDetailScreen(
    customerId: Long,
    viewModel: BahiKhataViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToTransactionEntry: (Long, String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMonthlyReport: () -> Unit
) {
    val context = LocalContext.current
    val initialCustomer = remember(customerId) { viewModel.getCustomerFromCache(customerId) }
    val customerFlow = remember(customerId) { viewModel.getCustomerById(customerId) }
    val customer by customerFlow.collectAsStateWithLifecycle(initialValue = initialCustomer)

    val transactionsFlow = remember(customerId) { viewModel.getTransactionsForCustomer(customerId) }
    val transactions by transactionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedMonthFilter by remember { mutableStateOf<String?>(null) } // null means "All Time"
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var actionMenuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCallConfirmDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val pastMonths = remember { DateTimeUtils.getPastMonths(6) }

    val currentCustomer = customer

    // Filter transactions by selected month if present
    val filteredTxns = remember(transactions, selectedMonthFilter) {
        if (selectedMonthFilter != null) {
            transactions.filter { it.entryDate.startsWith(selectedMonthFilter!!) }
        } else {
            transactions
        }
    }

    // Totals for all time
    val totalAllUdhar = remember(transactions) { transactions.filter { it.isCredit }.sumOf { it.amountPaise } }
    val totalAllJama = remember(transactions) { transactions.filter { it.isPayment }.sumOf { it.amountPaise } }
    val netAllBakaya = remember(totalAllUdhar, totalAllJama) { totalAllUdhar - totalAllJama }

    // Totals for selected month
    val totalMonthUdhar = remember(filteredTxns) { filteredTxns.filter { it.isCredit }.sumOf { it.amountPaise } }
    val totalMonthJama = remember(filteredTxns) { filteredTxns.filter { it.isPayment }.sumOf { it.amountPaise } }

    // Group transactions by date
    val groupedTxns = remember(filteredTxns) { filteredTxns.groupBy { it.entryDate } }

    Scaffold(
        topBar = {
            BahiKhataTopBar(
                title = currentCustomer?.name ?: "ग्राहक विवरण",
                subtitle = currentCustomer?.let { "ग्राहक कोड: ${it.customerCode}" } ?: "विवरण लोड हो रहा है...",
                showBack = true,
                onBackClick = onNavigateBack,
                onHomeClick = onNavigateHome,
                containerColor = TopBarLightPurpleBg,
                contentColor = TopBarLightPurpleContent,
                borderColor = TopBarLightPurpleBorder,
                accentIconTint = TopBarLightPurpleAccent,
                actions = {
                    if (currentCustomer != null) {
                        Box {
                            IconButton(
                                onClick = { actionMenuExpanded = true },
                                modifier = Modifier.testTag("customer_more_actions_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "अधिक विकल्प",
                                    tint = TopBarLightPurpleContent
                                )
                            }

                            DropdownMenu(
                                expanded = actionMenuExpanded,
                                onDismissRequest = { actionMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("विवरण संपादित करें (Edit)") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        actionMenuExpanded = false
                                        showEditDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ग्राहक हटाएं (Delete)", color = UdharRed) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = UdharRed) },
                                    onClick = {
                                        actionMenuExpanded = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentCustomer != null) {
                FloatingActionButton(
                    onClick = { onNavigateToTransactionEntry(currentCustomer.id, "credit") },
                    containerColor = LedgerRedPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("customer_add_transaction_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "नया लेन-देन")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "नया हिसाब दर्ज करें", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        },
        containerColor = WarmCanvasBg
    ) { innerPadding ->
        if (currentCustomer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ग्राहक डेटा लोड हो रहा है...",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextDarkSecondary)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
            // 1. CUSTOMER PROFILE HEADER CARD (Auto-hiding optional fields)
            item {
                CustomerProfileCard(
                    customer = currentCustomer,
                    onCallCustomer = {
                        if (!currentCustomer.mobileNumber.isNullOrBlank()) {
                            showCallConfirmDialog = true
                        }
                    }
                )
            }

            // 2. TOTAL BAKAYA SUMMARY RIBBON
            item {
                TotalBakayaRibbon(netBakaya = netAllBakaya)
            }

            // 3. MONTH FILTER & PDF DOWNLOAD ACTION BAR
            item {
                MonthFilterAndPdfBar(
                    selectedMonth = selectedMonthFilter,
                    pastMonths = pastMonths,
                    menuExpanded = monthMenuExpanded,
                    onToggleMenu = { monthMenuExpanded = it },
                    onSelectMonth = {
                        selectedMonthFilter = it
                        monthMenuExpanded = false
                    },
                    onExportPdf = { viewModel.exportCustomerLedgerPdf(currentCustomer, selectedMonthFilter) }
                )
            }

            // 4. MONTHLY TOTALS (Udhar vs Jama)
            item {
                MonthlyTotalsCard(
                    monthTitle = if (selectedMonthFilter != null) DateTimeUtils.formatMonthToHindi(selectedMonthFilter!!) else "अब-तक",
                    creditPaise = totalMonthUdhar,
                    paymentPaise = totalMonthJama
                )
            }

            // 5. TRANSACTION HISTORY LIST HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "खाता प्रविष्टियां (${filteredTxns.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    )
                    Text(
                        text = "दिनांक अनुसार",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }

            // 6. DATE-GROUPED TRANSACTIONS
            if (filteredTxns.isEmpty()) {
                item {
                    EmptyTransactionsPlaceholder(
                        onAddTransaction = { onNavigateToTransactionEntry(currentCustomer.id, "credit") }
                    )
                }
            } else {
                groupedTxns.forEach { (date, dateTxns) ->
                    item {
                        DateSectionHeader(date = date)
                    }

                    items(dateTxns, key = { it.id }) { txn ->
                        TransactionRowItem(
                            transaction = txn,
                            onDelete = { transactionToDelete = txn }
                        )
                    }
                }
            }
        }
    }

    // Edit Customer Dialog
    if (showEditDialog && currentCustomer != null) {
        EditCustomerDialog(
            customer = currentCustomer,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedName, updatedMobile, updatedAddress ->
                viewModel.updateCustomer(
                    currentCustomer.copy(
                        name = updatedName,
                        mobileNumber = updatedMobile?.ifBlank { null },
                        address = updatedAddress?.ifBlank { null }
                    )
                )
                showEditDialog = false
            }
        )
    }

    // Delete Customer Confirmation Dialog
    if (showDeleteConfirmDialog && currentCustomer != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("ग्राहक हटाएं?", fontWeight = FontWeight.Bold, color = UdharRed) },
            text = {
                Text("क्या आप सचमुच ग्राहक '${currentCustomer.name}' और उनके सभी लेन-देन का इतिहास हटाना चाहते हैं?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(currentCustomer) {
                            showDeleteConfirmDialog = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UdharRed)
                ) {
                    Text("हटाएं (Delete)")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Call Confirmation Dialog
    if (showCallConfirmDialog && currentCustomer != null && !currentCustomer.mobileNumber.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showCallConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "कॉल करें",
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "क्या आप '${currentCustomer.name}' (${currentCustomer.mobileNumber}) को कॉल करना चाहते हैं?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextDarkPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCallConfirmDialog = false
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${currentCustomer.mobileNumber}"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.testTag("confirm_call_button")
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("कॉल करें")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCallConfirmDialog = false },
                    modifier = Modifier.testTag("dismiss_call_button")
                ) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Delete Single Transaction Dialog
    if (transactionToDelete != null) {
        val txn = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("प्रविष्टि हटाएं?", fontWeight = FontWeight.Bold, color = UdharRed) },
            text = {
                Text("क्या आप ${if (txn.isCredit) "उधार" else "जमा"} राशि ₹${CurrencyUtils.formatPaiseToRupees(txn.amountPaise, false)} की यह प्रविष्टि हटाना चाहते हैं?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(txn)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UdharRed)
                ) {
                    Text("हटाएं")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { transactionToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
}

@Composable
private fun CustomerProfileCard(
    customer: Customer,
    onCallCustomer: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    )
                }

                // Phone Call button if mobile present
                if (!customer.mobileNumber.isNullOrBlank()) {
                    IconButton(
                        onClick = onCallCustomer,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFE0F2FE), CircleShape)
                            .testTag("btn_call_customer")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "कॉल करें",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Optional Contact Info (Auto-hiding if blank)
            if (!customer.mobileNumber.isNullOrBlank() || !customer.address.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!customer.mobileNumber.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = customer.mobileNumber,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextDarkSecondary)
                            )
                        }
                    }

                    if (!customer.address.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = customer.address,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextDarkSecondary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalBakayaRibbon(netBakaya: Long) {
    val isDue = netBakaya > 0
    val isAdvance = netBakaya < 0
    val ribbonColor = when {
        isDue -> UdharRedBg
        isAdvance -> JamaGreenBg
        else -> Color(0xFFF8FAFC)
    }
    val textColor = when {
        isDue -> UdharRed
        isAdvance -> JamaGreen
        else -> Color.DarkGray
    }
    val statusText = when {
        isDue -> "कुल बाकी (उधार - जमा)"
        isAdvance -> "कुल अग्रिम (जमा - उधार )"
        else -> "खाता बराबर (Nil Balance)"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ribbonColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("ribbon_total_bakaya")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
                Text(
                    text = "समस्त लेन-देन का अंतिम शेष",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }

            Text(
                text = CurrencyUtils.formatPaiseToRupees(netBakaya),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
            )
        }
    }
}

@Composable
private fun MonthFilterAndPdfBar(
    selectedMonth: String?,
    pastMonths: List<Pair<String, String>>,
    menuExpanded: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    onSelectMonth: (String?) -> Unit,
    onExportPdf: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Month Dropdown Trigger
        Box {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier
                    .clickable { onToggleMenu(true) }
                    .testTag("customer_month_filter_dropdown")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = LedgerRedPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedMonth != null) DateTimeUtils.formatMonthToHindi(selectedMonth) else "सभी माह (All Time)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onToggleMenu(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("सभी माह (All Time)", fontWeight = if (selectedMonth == null) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { onSelectMonth(null) }
                )
                pastMonths.forEach { (dbFormat, hindiLabel) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = hindiLabel,
                                fontWeight = if (selectedMonth == dbFormat) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedMonth == dbFormat) LedgerRedPrimary else Color.Unspecified
                            )
                        },
                        onClick = { onSelectMonth(dbFormat) }
                    )
                }
            }
        }

        // PDF Export Button
        Button(
            onClick = onExportPdf,
            colors = ButtonDefaults.buttonColors(containerColor = LedgerRedPrimary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.testTag("btn_export_customer_pdf")
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "PDF Report", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MonthlyTotalsCard(
    monthTitle: String,
    creditPaise: Long,
    paymentPaise: Long
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$monthTitle का हिसाब",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "उधार",
                        style = MaterialTheme.typography.labelSmall.copy(color = UdharRed)
                    )
                    Text(
                        text = CurrencyUtils.formatPaiseToRupees(creditPaise),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = UdharRed
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "जमा",
                        style = MaterialTheme.typography.labelSmall.copy(color = JamaGreen)
                    )
                    Text(
                        text = CurrencyUtils.formatPaiseToRupees(paymentPaise),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = JamaGreen
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSectionHeader(date: String) {
    Surface(
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = DateTimeUtils.formatToHindiDate(date),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TransactionRowItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    val isCredit = transaction.isCredit
    val amtColor = if (isCredit) UdharRed else JamaGreen
    val typeBadgeBg = if (isCredit) UdharRedBg else JamaGreenBg
    val typeText = if (isCredit) "उधार (-)" else "जमा (+)"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .testTag("txn_row_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Type badge
                Surface(
                    color = typeBadgeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = typeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = amtColor,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = transaction.description?.ifBlank { if (isCredit) "उधार सामान" else "नकद जमा" } ?: (if (isCredit) "उधार सामान" else "नकद जमा"),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextDarkPrimary
                        ),
                        maxLines = 2
                    )
                    Text(
                        text = DateTimeUtils.formatTo12HourTime(transaction.entryTime),
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CurrencyUtils.formatPaiseToRupees(transaction.amountPaise),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = amtColor
                    )
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(start = 4.dp)
                        .testTag("btn_delete_txn_${transaction.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "प्रविष्टि हटाएं",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsPlaceholder(onAddTransaction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "इस अवधि में कोई प्रविष्टि नहीं है",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "नया उधार या जमा दर्ज करने के लिए नीचे बटन दबाएं",
                style = MaterialTheme.typography.bodySmall.copy(color = TextDarkSecondary),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddTransaction,
                colors = ButtonDefaults.buttonColors(containerColor = LedgerRedPrimary)
            ) {
                Text("+ नया लेन-देन दर्ज करें")
            }
        }
    }
}

@Composable
private fun EditCustomerDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (name: String, mobile: String?, address: String?) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var mobile by remember { mutableStateOf(customer.mobileNumber ?: "") }
    var address by remember { mutableStateOf(customer.address ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var mobileError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ग्राहक विवरण संपादित करें", fontWeight = FontWeight.Bold, color = LedgerRedPrimary) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError && it.isNotBlank()) nameError = false
                    },
                    label = { Text("ग्राहक का नाम *") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("ग्राहक का नाम अनिवार्य है", color = UdharRed) } } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(10)
                        mobile = digits
                        if (mobileError && (digits.isEmpty() || digits.length == 10)) mobileError = false
                    },
                    label = { Text("मोबाइल नंबर (10 अंक)") },
                    isError = mobileError,
                    supportingText = if (mobileError) {
                        { Text("मोबाइल नंबर ठीक 10 अंकों का होना चाहिए", color = UdharRed) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("पता") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedMobile = mobile.trim()
                    val isNameValid = trimmedName.isNotBlank()
                    val isMobileValid = trimmedMobile.isEmpty() || (trimmedMobile.length == 10 && trimmedMobile.all { it.isDigit() })

                    nameError = !isNameValid
                    mobileError = !isMobileValid

                    if (isNameValid && isMobileValid) {
                        onConfirm(trimmedName, trimmedMobile.ifBlank { null }, address.trim().ifBlank { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LedgerRedPrimary)
            ) {
                Text("सुरक्षित करें")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
