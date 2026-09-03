package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppSetting
import com.example.data.model.CustomerWithSummary
import com.example.data.model.DailySummary
import com.example.ui.viewmodel.CustomerSortOption
import com.example.ui.components.AddCustomerDialog
import com.example.ui.components.BahiKhataTopBar
import com.example.ui.components.TopBarLightRedBg
import com.example.ui.components.TopBarLightRedBorder
import com.example.ui.components.TopBarLightRedContent
import com.example.ui.theme.BorderLight
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.JamaGreenBg
import com.example.ui.theme.LedgerRedPrimary
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.UdharRed
import com.example.ui.theme.UdharRedBg
import com.example.ui.theme.WarmCanvasBg
import com.example.ui.viewmodel.BahiKhataViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateTimeUtils
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: BahiKhataViewModel,
    onNavigateToCustomerDetail: (Long) -> Unit,
    onNavigateToTransactionEntry: (Long?, String) -> Unit,
    onNavigateToMonthlyReport: () -> Unit,
    onNavigateToCustomerList: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelpFaq: () -> Unit
) {
    val context = LocalContext.current

    val customersList by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val dailySummary by viewModel.dailySummary.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDashboardDate.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val shopSettings by viewModel.shopSettings.collectAsStateWithLifecycle()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val shopName = shopSettings[AppSetting.KEY_SHOP_NAME]?.ifBlank { "डिजिटल बही-खाता" } ?: "डिजिटल बही-खाता"
    val isToday = selectedDate == DateTimeUtils.getTodayDateDb()

    Scaffold(
        topBar = {
            BahiKhataTopBar(
                title = shopName,
                subtitle = "दैनिक हिसाब एवं खाता-बही",
                showBack = false,
                onSettingsClick = onNavigateToSettings,
                onHelpClick = onNavigateToHelpFaq,
                containerColor = TopBarLightRedBg,
                contentColor = TopBarLightRedContent,
                borderColor = TopBarLightRedBorder
            )
        },
        containerColor = WarmCanvasBg
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. DATE SELECTOR BANNER
            item {
                DateSelectorBar(
                    selectedDate = selectedDate,
                    isToday = isToday,
                    onPrevDay = { viewModel.stepDashboardDate(-1) },
                    onNextDay = { viewModel.stepDashboardDate(1) },
                    onPickDate = {
                        val cal = Calendar.getInstance()
                        val parts = selectedDate.split("-")
                        val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
                        val m = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
                        val d = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

                        val picker = DatePickerDialog(context, { _, year, month, dayOfMonth ->
                            val formatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            viewModel.setSelectedDashboardDate(formatted)
                        }, y, m, d)
                        picker.datePicker.maxDate = System.currentTimeMillis()
                        picker.show()
                    },
                    onJumpToday = { viewModel.setSelectedDashboardDate(DateTimeUtils.getTodayDateDb()) }
                )
            }

            // 2. DAILY SUMMARY CARDS & QUICK ACCESS FRAMES
            item {
                SummaryCardsSection(
                    summary = dailySummary,
                    isToday = isToday,
                    onNavigateToCustomerList = onNavigateToCustomerList
                )
            }

            // 3. ACTION BUTTONS: [ADD NEW CUSTOMER] (LEFT) + [MONTHLY REPORT] (RIGHT) (JUST ABOVE SEARCH BOX)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // LEFT: नया ग्राहक
                    Surface(
                        onClick = { showAddCustomerDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_add_customer_fab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = LedgerRedPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "नया ग्राहक जोड़ें",
                                        tint = LedgerRedPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "+ नया ग्राहक",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = LedgerRedPrimary,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "खाता जोड़ें",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextDarkSecondary,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // RIGHT: मासिक रिपोर्ट
                    Surface(
                        onClick = onNavigateToMonthlyReport,
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_masik_report_home")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEA580C).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Summarize,
                                        contentDescription = "मासिक रिपोर्ट",
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "मासिक रिपोर्ट",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEA580C),
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "पूरा हिसाब-किताब",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextDarkSecondary,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 4. SEARCH BAR & SORT DROPDOWN
            item {
                SearchAndSortBar(
                    query = searchQuery,
                    onQueryChange = {
                        viewModel.setSearchQuery(it)
                        if (it.isNotEmpty()) {
                            coroutineScope.launch { listState.animateScrollToItem(index = 3) }
                        }
                    },
                    onSearchFocus = {
                        coroutineScope.launch { listState.animateScrollToItem(index = 3) }
                    },
                    sortOption = sortOption,
                    sortMenuExpanded = sortMenuExpanded,
                    onSortMenuToggle = { sortMenuExpanded = it },
                    onSortOptionSelect = {
                        viewModel.setSortOption(it)
                        sortMenuExpanded = false
                    }
                )
            }

            // 5. CUSTOMER LIST HEADER WITH TOTAL COUNT
            item {
                val dateLabel = if (isToday) "आज" else DateTimeUtils.formatToHindiDate(selectedDate)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ग्राहक सूची (${customersList.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    )
                    Text(
                        text = "$dateLabel का दैनिक लेन-देन",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LedgerRedPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // 6. CUSTOMER LIST ITEMS (WITH SERIAL NUMBER & DAILY VALUES)
            if (customersList.isEmpty()) {
                item {
                    EmptyCustomerPlaceholder(
                        searchQuery = searchQuery,
                        onAddCustomer = { showAddCustomerDialog = true }
                    )
                }
            } else {
                items(customersList, key = { it.customer.id }) { item ->
                    CustomerCardRow(
                        item = item,
                        isToday = isToday,
                        onViewDetail = { onNavigateToCustomerDetail(item.customer.id) },
                        onQuickEntry = { onNavigateToTransactionEntry(item.customer.id, "credit") }
                    )
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, mobile, address ->
                viewModel.addNewCustomer(name, mobile, address) { newCustomer ->
                    showAddCustomerDialog = false
                    onNavigateToCustomerDetail(newCustomer.id)
                }
            }
        )
    }
}

@Composable
private fun DateSelectorBar(
    selectedDate: String,
    isToday: Boolean,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    onJumpToday: () -> Unit
) {
    val canGoForward = !isToday && selectedDate < DateTimeUtils.getTodayDateDb()

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
                onClick = onPrevDay,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("date_prev_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "पिछला दिन",
                    tint = LedgerRedPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPickDate() }
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "कैलेंडर",
                    tint = LedgerRedPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = DateTimeUtils.formatToHindiDate(selectedDate),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isToday) {
                    TextButton(
                        onClick = onJumpToday,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("date_today_jump_button")
                    ) {
                        Text(
                            text = "आज",
                            color = LedgerRedPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (canGoForward) onNextDay()
                    },
                    enabled = canGoForward,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("date_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "अगला दिन",
                        tint = if (canGoForward) LedgerRedPrimary else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCardsSection(
    summary: DailySummary,
    isToday: Boolean,
    onNavigateToCustomerList: () -> Unit
) {
    val dateLabel = if (isToday) "आज" else "चयनित दिन"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Prominent 2-Card Row: Daily Udhar & Daily Jama (Reduced height, no placeholder subtitle)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Daily Credit (Udhar)
            Card(
                colors = CardDefaults.cardColors(containerColor = UdharRedBg),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, UdharRed.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_today_udhar")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = "$dateLabel का उधार",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = UdharRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatPaiseToRupees(summary.todayCreditPaise),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = UdharRed,
                            fontSize = 19.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Card 2: Daily Payment (Jama)
            Card(
                colors = CardDefaults.cardColors(containerColor = JamaGreenBg),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, JamaGreen.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_today_jama")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = "$dateLabel का जमा",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = JamaGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatPaiseToRupees(summary.todayPaymentPaise),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = JamaGreen,
                            fontSize = 19.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 2 Frames below Udhar & Jama values:
        // 1st Frame -> 'ग्राहक प्रविष्टियाँ' (total no. of customer who has any entries on that day, read-only)
        // 2nd Frame -> 'ग्राहक सूची' ('grahak suchi', clickable button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1st Frame: ग्राहक प्रविष्टियाँ (Read-Only)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("frame_grahak_pravistiyan")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "ग्राहक प्रविष्टियाँ",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ग्राहक प्रविष्टियाँ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "${summary.activeCustomersCount}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                fontSize = 17.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            // 2nd Frame: ग्राहक सूची (Clickable Button)
            Surface(
                onClick = onNavigateToCustomerList,
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .weight(1f)
                    .testTag("frame_grahak_suchi_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF334155).copy(alpha = 0.10f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "ग्राहक सूची",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ग्राहक सूची",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "सभी ग्राहक ➔",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAndSortBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchFocus: () -> Unit = {},
    sortOption: CustomerSortOption,
    sortMenuExpanded: Boolean,
    onSortMenuToggle: (Boolean) -> Unit,
    onSortOptionSelect: (CustomerSortOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("नाम, क्रमांक या कोड खोजें...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "खोजें",
                    tint = LedgerRedPrimary
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "हटाएं",
                            tint = Color.Gray
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LedgerRedPrimary,
                unfocusedBorderColor = BorderLight,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged {
                    if (it.isFocused) {
                        onSearchFocus()
                    }
                }
                .testTag("home_search_input")
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            OutlinedButton(
                onClick = { onSortMenuToggle(true) },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderLight),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                modifier = Modifier.testTag("home_sort_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "क्रमबद्ध करें",
                    tint = LedgerRedPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { onSortMenuToggle(false) }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "क्रम संख्या अनुसार (1, 2, 3..)",
                            fontWeight = if (sortOption == CustomerSortOption.SERIAL_NO) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { onSortOptionSelect(CustomerSortOption.SERIAL_NO) }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "नाम के अनुसार (A to Z)",
                            fontWeight = if (sortOption == CustomerSortOption.NAME_ASC) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { onSortOptionSelect(CustomerSortOption.NAME_ASC) }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "आज के उधार अनुसार",
                            fontWeight = if (sortOption == CustomerSortOption.DAILY_UDHAR) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { onSortOptionSelect(CustomerSortOption.DAILY_UDHAR) }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "आज के जमा अनुसार",
                            fontWeight = if (sortOption == CustomerSortOption.DAILY_JAMA) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { onSortOptionSelect(CustomerSortOption.DAILY_JAMA) }
                )
            }
        }
    }
}

@Composable
private fun CustomerCardRow(
    item: CustomerWithSummary,
    isToday: Boolean,
    onViewDetail: () -> Unit,
    onQuickEntry: () -> Unit
) {
    val dateLabel = if (isToday) "आज" else "दिन"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onViewDetail() }
            .testTag("customer_row_${item.customer.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Serial No. (e.g. (1), (2)), Name, Active on Selected Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Serial Number Badge (Replaces customer code as requested)
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "(${item.serialNumber})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = LedgerRedPrimary,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Green Checkmark if active on selected date
                    if (item.activeOnSelectedDate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "इस दिन सक्रिय लेन-देन",
                            tint = JamaGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Selected Date's Daily Udhar & Daily Jama values + Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (item.activeOnSelectedDate || item.selectedDateCreditPaise > 0 || item.selectedDatePaymentPaise > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (item.selectedDateCreditPaise > 0) {
                            Surface(
                                color = UdharRedBg,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "उधार: ${CurrencyUtils.formatPaiseToRupees(item.selectedDateCreditPaise, includeDecimal = false)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = UdharRed,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (item.selectedDatePaymentPaise > 0) {
                            Surface(
                                color = JamaGreenBg,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "जमा: ${CurrencyUtils.formatPaiseToRupees(item.selectedDatePaymentPaise, includeDecimal = false)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = JamaGreen,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "इस दिन कोई लेन-देन नहीं",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    )
                }

                // Quick Entry Button (clicking on the card opens customer details directly)
                Button(
                    onClick = onQuickEntry,
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerRedPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_quick_entry_${item.customer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "लेन-देन", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EmptyCustomerPlaceholder(
    searchQuery: String,
    onAddCustomer: () -> Unit
) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (searchQuery.isNotBlank()) "कोई ग्राहक नहीं मिला" else "अभी कोई ग्राहक नहीं है",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "कृपया अन्य नाम से खोजें" else "नया ग्राहक जोड़ने के लिए नीचे दिए गए बटन पर टैप करें",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAddCustomer,
                colors = ButtonDefaults.buttonColors(containerColor = LedgerRedPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("नया ग्राहक जोड़ें")
            }
        }
    }
}