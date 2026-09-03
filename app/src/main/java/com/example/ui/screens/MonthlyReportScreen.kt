package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerMonthlyBreakdown
import com.example.data.model.MonthlySummary
import com.example.ui.components.BahiKhataTopBar
import com.example.ui.components.TopBarLightOrangeAccent
import com.example.ui.components.TopBarLightOrangeBg
import com.example.ui.components.TopBarLightOrangeBorder
import com.example.ui.components.TopBarLightOrangeContent
import com.example.ui.theme.BorderLight
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.JamaGreenBg
import com.example.ui.theme.LedgerRedPrimary
import com.example.ui.theme.SaffronGoldContainer
import com.example.ui.theme.SaffronGoldDark
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.UdharRed
import com.example.ui.theme.UdharRedBg
import com.example.ui.theme.WarmCanvasBg
import com.example.ui.viewmodel.BahiKhataViewModel
import com.example.ui.viewmodel.MonthlyBreakdownSort
import com.example.util.CurrencyUtils
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    viewModel: BahiKhataViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToCustomerDetail: (Long) -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthlySummary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val breakdowns by viewModel.monthlyCustomerBreakdowns.collectAsStateWithLifecycle()
    val breakdownSort by viewModel.monthlyBreakdownSort.collectAsStateWithLifecycle()

    var monthMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showPdfOptionDialog by remember { mutableStateOf(false) }

    val pastMonths = remember { DateTimeUtils.getPastMonths(12) }

    Scaffold(
        topBar = {
            BahiKhataTopBar(
                title = "मासिक हिसाब रिपोर्ट",
                subtitle = DateTimeUtils.formatMonthToHindi(selectedMonth),
                showBack = true,
                onBackClick = onNavigateBack,
                onHomeClick = onNavigateHome,
                containerColor = TopBarLightOrangeBg,
                contentColor = TopBarLightOrangeContent,
                borderColor = TopBarLightOrangeBorder,
                accentIconTint = TopBarLightOrangeAccent
            )
        },
        containerColor = WarmCanvasBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // 1. MONTH SELECTOR STRIP
            item {
                MonthSelectorStrip(
                    selectedMonth = selectedMonth,
                    pastMonths = pastMonths,
                    menuExpanded = monthMenuExpanded,
                    onToggleMenu = { monthMenuExpanded = it },
                    onSelectMonth = {
                        viewModel.setSelectedMonth(it)
                        monthMenuExpanded = false
                    },
                    onPrevMonth = {
                        val currentIndex = pastMonths.indexOfFirst { it.first == selectedMonth }
                        if (currentIndex >= 0 && currentIndex < pastMonths.size - 1) {
                            viewModel.setSelectedMonth(pastMonths[currentIndex + 1].first)
                        }
                    },
                    onNextMonth = {
                        val currentIndex = pastMonths.indexOfFirst { it.first == selectedMonth }
                        if (currentIndex > 0) {
                            viewModel.setSelectedMonth(pastMonths[currentIndex - 1].first)
                        }
                    }
                )
            }

            // 2. MONTHLY SUMMARY METRICS CARDS
            item {
                MonthlyOverviewCards(summary = monthlySummary)
            }

            // 3. PDF EXPORT ACTION BANNER
            item {
                PdfExportBanner(onOpenPdfDialog = { showPdfOptionDialog = true })
            }

            // 4. CUSTOMER BREAKDOWN HEADER WITH SORT
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ग्राहकों का मासिक विवरण (${breakdowns.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    )

                    Box {
                        OutlinedButton(
                            onClick = { sortMenuExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("monthly_sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = LedgerRedPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = breakdownSort.titleHindi,
                                fontSize = 11.sp,
                                color = LedgerRedPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            MonthlyBreakdownSort.values().forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sort.titleHindi,
                                            fontWeight = if (sort == breakdownSort) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sort == breakdownSort) LedgerRedPrimary else Color.Unspecified
                                        )
                                    },
                                    onClick = {
                                        viewModel.setMonthlyBreakdownSort(sort)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. BREAKDOWN LIST
            if (breakdowns.isEmpty()) {
                item {
                    EmptyMonthPlaceholder(monthTitle = DateTimeUtils.formatMonthToHindi(selectedMonth))
                }
            } else {
                items(breakdowns, key = { it.customer.id }) { item ->
                    CustomerMonthlyCard(
                        breakdown = item,
                        onClick = { onNavigateToCustomerDetail(item.customer.id) }
                    )
                }
            }
        }
    }

    // PDF Export Options Dialog
    if (showPdfOptionDialog) {
        PdfExportChoiceDialog(
            monthTitle = DateTimeUtils.formatMonthToHindi(selectedMonth),
            onDismiss = { showPdfOptionDialog = false },
            onExportTransactions = {
                showPdfOptionDialog = false
                viewModel.exportMonthlyTransactionsPdf(selectedMonth)
            },
            onExportVouchers = {
                showPdfOptionDialog = false
                viewModel.exportMonthlyCustomerVouchersPdf(selectedMonth)
            }
        )
    }
}

@Composable
private fun MonthSelectorStrip(
    selectedMonth: String,
    pastMonths: List<Pair<String, String>>,
    menuExpanded: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    onSelectMonth: (String) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val currentIndex = pastMonths.indexOfFirst { it.first == selectedMonth }
    val canGoForward = currentIndex > 0
    val canGoBack = currentIndex < pastMonths.size - 1

    Surface(
        color = Color(0xFFFAF5E4),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrevMonth,
                enabled = canGoBack,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("month_prev_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "पिछला महीना",
                    tint = if (canGoBack) LedgerRedPrimary else Color.LightGray
                )
            }

            Box {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    modifier = Modifier
                        .clickable { onToggleMenu(true) }
                        .testTag("month_selector_dropdown")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                            text = DateTimeUtils.formatMonthToHindi(selectedMonth),
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

            IconButton(
                onClick = { if (canGoForward) onNextMonth() },
                enabled = canGoForward,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("month_next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "अगला महीना",
                    tint = if (canGoForward) LedgerRedPrimary else Color.LightGray
                )
            }
        }
    }
}

@Composable
private fun MonthlyOverviewCards(summary: MonthlySummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Row 1: Monthly Udhar & Monthly Jama
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = UdharRedBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "माह का कुल उधार",
                        style = MaterialTheme.typography.labelSmall.copy(color = UdharRed, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatPaiseToRupees(summary.totalCreditPaise),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = UdharRed,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = JamaGreenBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "माह का कुल जमा",
                        style = MaterialTheme.typography.labelSmall.copy(color = JamaGreen, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatPaiseToRupees(summary.totalPaymentPaise),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = JamaGreen,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Net Difference & Active Days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SaffronGoldContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "मासिक अंतर (Net Flow)",
                        style = MaterialTheme.typography.labelSmall.copy(color = SaffronGoldDark, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = (if (summary.netDifferencePaise > 0) "+ " else "") + CurrencyUtils.formatPaiseToRupees(summary.netDifferencePaise),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (summary.netDifferencePaise >= 0) SaffronGoldDark else JamaGreen,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "लेन-देन दिवस",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${summary.transactionDaysCount} दिन (${summary.totalTransactionsCount} एंट्री)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E40AF),
                            fontSize = 14.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfExportBanner(onOpenPdfDialog: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onOpenPdfDialog() }
            .testTag("banner_pdf_export")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = LedgerRedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "PDF रिपोर्ट डाउनलोड करें",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    )
                    // Text(
                    //     text = "सभी लेन-देन या ग्राहक बिल वाउचर PDF में सुरक्षित करें",
                    //     style = MaterialTheme.typography.bodySmall.copy(color = TextDarkSecondary)
                    // )
                }
            }

            Button(
                onClick = onOpenPdfDialog,
                colors = ButtonDefaults.buttonColors(containerColor = LedgerRedPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("PDF निकालें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CustomerMonthlyCard(
    breakdown: CustomerMonthlyBreakdown,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onClick() }
            .testTag("monthly_cust_card_${breakdown.customer.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Serial No. & Customer Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "(${breakdown.serialNumber})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = breakdown.customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Monthly Udhar & Monthly Jama (shifted to right corner, entry count omitted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "मासिक उधार: ${CurrencyUtils.formatPaiseToRupees(breakdown.monthlyCreditPaise)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = UdharRed, fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "मासिक जमा: ${CurrencyUtils.formatPaiseToRupees(breakdown.monthlyPaymentPaise)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = JamaGreen, fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Row 3: Pichla Bakaya & Udhar Dey (in same row with exact color scheme)
            val pichlaColor = when {
                breakdown.pichlaBakayaPaise > 0 -> UdharRed
                breakdown.pichlaBakayaPaise < 0 -> JamaGreen
                else -> TextMuted
            }
            val pichlaText = when {
                breakdown.pichlaBakayaPaise > 0 -> "+ ${CurrencyUtils.formatPaiseToRupees(breakdown.pichlaBakayaPaise)}"
                breakdown.pichlaBakayaPaise < 0 -> "- ${CurrencyUtils.formatPaiseToRupees(kotlin.math.abs(breakdown.pichlaBakayaPaise))}"
                else -> "₹0"
            }

            val prabhaviColor = when {
                breakdown.prabhaviUdharPaise > 0 -> UdharRed
                breakdown.prabhaviUdharPaise < 0 -> JamaGreen
                else -> TextMuted
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "पिछला बकाया: $pichlaText",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = pichlaColor,
                        fontWeight = if (breakdown.pichlaBakayaPaise != 0L) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
                Text(
                    text = "उधार देय: ${CurrencyUtils.formatPaiseToRupees(breakdown.prabhaviUdharPaise)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = prabhaviColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyMonthPlaceholder(monthTitle: String) {
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
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "$monthTitle में कोई लेन-देन दर्ज नहीं हुआ है",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDarkSecondary
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PdfExportChoiceDialog(
    monthTitle: String,
    onDismiss: () -> Unit,
    onExportTransactions: () -> Unit,
    onExportVouchers: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "मासिक PDF रिपोर्ट चुनें ($monthTitle)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = LedgerRedPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option 1: All Monthly Transactions
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportTransactions() }
                        .testTag("dialog_pdf_all_txns")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = null,
                            tint = LedgerRedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "1. सभी ग्राहकों की मासिक रिपोर्ट (सारणी प्रारूप)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "सभी ग्राहकों का सारणी विवरण (क्र., कोड, नाम, मासिक उधार, पिछला बकाया, उधार देय, मासिक जमा, लेन-देन)",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextDarkSecondary)
                            )
                        }
                    }
                }

                // Option 2: Customer Bill Vouchers Table Grid
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportVouchers() }
                        .testTag("dialog_pdf_vouchers")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "2. [PART-B] ग्राहक बिल वाउचर (3-Column Grid)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "प्रत्येक ग्राहक का मासिक उधार व हस्ताक्षर हेतु 3-कॉलम प्रिंट पर्ची ग्रिड",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextDarkSecondary)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("बंद करें")
            }
        }
    )
}
