package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerWithSummary
import com.example.ui.components.BahiKhataTopBar
import com.example.ui.components.TopBarLightGreyAccent
import com.example.ui.components.TopBarLightGreyBg
import com.example.ui.components.TopBarLightGreyBorder
import com.example.ui.components.TopBarLightGreyContent
import com.example.ui.theme.WarmCanvasBg
import com.example.ui.viewmodel.BahiKhataViewModel

enum class GrahakSuchiSortOption(val displayName: String) {
    SERIAL_ASC("क्रमांक (1 ➔ N)"),
    SERIAL_DESC("क्रमांक (N ➔ 1)"),
    NAME_ASC("नाम (A ➔ Z)"),
    NAME_DESC("नाम (Z ➔ A)"),
    CODE_ASC("ग्राहक कोड (CUS)")
}

@Composable
fun CustomerListScreen(
    viewModel: BahiKhataViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val customersWithSummary by viewModel.allCustomersWithSummary.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(GrahakSuchiSortOption.SERIAL_ASC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Independent sort and filter for CustomerListScreen (does not depend on home screen's filter)
    val sortedAndFilteredList by remember(customersWithSummary, searchQuery, selectedSort) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                when (selectedSort) {
                    GrahakSuchiSortOption.SERIAL_ASC -> customersWithSummary.sortedBy { it.serialNumber }
                    GrahakSuchiSortOption.SERIAL_DESC -> customersWithSummary.sortedByDescending { it.serialNumber }
                    GrahakSuchiSortOption.NAME_ASC -> customersWithSummary.sortedBy { it.customer.name.lowercase() }
                    GrahakSuchiSortOption.NAME_DESC -> customersWithSummary.sortedByDescending { it.customer.name.lowercase() }
                    GrahakSuchiSortOption.CODE_ASC -> customersWithSummary.sortedBy { it.customer.customerCode.lowercase() }
                }
            } else {
                val matchedWithScore = customersWithSummary.mapNotNull { item ->
                    val score = item.getSearchRelevanceScore(searchQuery)
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
        }
    }

    // Scroll to top immediately when search or sort changes
    LaunchedEffect(searchQuery, selectedSort) {
        if (sortedAndFilteredList.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            BahiKhataTopBar(
                title = "ग्राहक सूची",
                subtitle = "सभी ग्राहकों का क्रमांक एवं कोड विवरण",
                showBack = true,
                onBackClick = onNavigateBack,
                onHomeClick = onNavigateHome,
                containerColor = TopBarLightGreyBg,
                contentColor = TopBarLightGreyContent,
                iconTint = TopBarLightGreyContent,
                accentIconTint = TopBarLightGreyAccent,
                borderColor = TopBarLightGreyBorder
            )
        },
        containerColor = WarmCanvasBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // SEARCH & SORT CONTROLS
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Search Field (by Name, Serial No, Customer Code) - Perfectly centered
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "खोजें",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "नाम / क्रमांक / कोड से खोजें...",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1E293B)
                                        ),
                                        cursorBrush = SolidColor(Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("grahak_suchi_search_input")
                                    )
                                }

                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "साफ़ करें",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Sort Menu Dropdown Button
                        Box {
                            OutlinedButton(
                                onClick = { sortMenuExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFF8FAFC)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("grahak_suchi_sort_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "क्रमबद्ध करें",
                                    tint = Color(0xFF334155),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "क्रम",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                GrahakSuchiSortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.displayName,
                                                fontWeight = if (selectedSort == option) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedSort == option) Color(0xFF0F172A) else Color(0xFF334155),
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            selectedSort = option
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Total Count & Active Filter Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "कुल ग्राहक: ${sortedAndFilteredList.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "क्रम: ${selectedSort.displayName}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // TABLE HEADER ROW (क्र. | ग्राहक का नाम | ग्राहक कोड)
            Surface(
                color = Color(0xFFE2E8F0),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "क्र.",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF334155),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.width(32.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "ग्राहक का पूरा नाम",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF334155),
                            fontSize = 12.5.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "ग्राहक कोड",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF334155),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.width(72.dp)
                    )
                }
            }

            // LIST ITEMS (READ-ONLY COMPACT ROWS)
            if (sortedAndFilteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "कोई ग्राहक नहीं मिला" else "कोई ग्राहक उपलब्ध नहीं है",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                        )
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = "खोज शब्द बदलकर पुनः प्रयास करें",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B)
                                )
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(
                        items = sortedAndFilteredList,
                        key = { _, item -> item.customer.id }
                    ) { index, item ->
                        CompactCustomerRow(
                            item = item,
                            isEvenRow = index % 2 == 0
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCustomerRow(
    item: CustomerWithSummary,
    isEvenRow: Boolean
) {
    val rowBg = if (isEvenRow) Color.White else Color(0xFFF8FAFC)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("compact_customer_row_${item.customer.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. SERIAL NUMBER (क्र. - Compact)
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFE2E8F0),
            modifier = Modifier.width(32.dp)
        ) {
            Text(
                text = "${item.serialNumber}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 2. FULL NAME (& optional mobile number - Expanded Space)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.customer.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!item.customer.mobileNumber.isNullOrBlank()) {
                Text(
                    text = item.customer.mobileNumber,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 3. CUSTOMER CODE (ग्राहक कोड - Compact)
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(0.5.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.width(72.dp)
        ) {
            Text(
                text = item.customer.customerCode,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155),
                    fontSize = 10.5.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp)
            )
        }
    }
}
