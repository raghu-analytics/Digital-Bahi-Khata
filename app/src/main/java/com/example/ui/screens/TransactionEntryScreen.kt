package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.components.TopBarLightBlueAccent
import com.example.ui.components.TopBarLightBlueBg
import com.example.ui.components.TopBarLightBlueBorder
import com.example.ui.components.TopBarLightBlueContent
import com.example.ui.components.TransactionConfirmationDialog
import com.example.ui.theme.BorderLight
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.JamaGreenBg
import com.example.ui.theme.LedgerRedDark
import com.example.ui.theme.LedgerRedPrimary
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.UdharRed
import com.example.ui.theme.UdharRedBg
import com.example.ui.theme.WarmCanvasBg
import com.example.ui.viewmodel.BahiKhataViewModel
import com.example.util.CalculatorEngine
import com.example.util.CurrencyUtils
import com.example.util.DateTimeUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryScreen(
    preselectedCustomerId: Long?,
    initialType: String,
    viewModel: BahiKhataViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToCustomerDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()

    var selectedCustomerId by remember {
        mutableStateOf(preselectedCustomerId ?: allCustomers.firstOrNull()?.id)
    }

    // If preselectedCustomerId was null but customers loaded
    if (selectedCustomerId == null && allCustomers.isNotEmpty()) {
        selectedCustomerId = allCustomers.first().id
    }

    var transactionType by remember {
        mutableStateOf(if (initialType == Transaction.TYPE_PAYMENT) Transaction.TYPE_PAYMENT else Transaction.TYPE_CREDIT)
    }
    var amountExpression by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var entryDate by remember { mutableStateOf(viewModel.selectedDashboardDate.value) }
    var entryTime by remember { mutableStateOf(DateTimeUtils.getCurrentTimeDb()) }

    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var confirmedTransaction by remember { mutableStateOf<Pair<Customer, Transaction>?>(null) }

    val currentCustomer = allCustomers.find { it.id == selectedCustomerId }

    val isCredit = transactionType == Transaction.TYPE_CREDIT
    val activeColor = if (isCredit) UdharRed else JamaGreen
    val activeBg = if (isCredit) UdharRedBg else JamaGreenBg

    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            BahiKhataTopBar(
                title = if (isCredit) "उधार एंट्री" else "जमा एंट्री",
                subtitle = currentCustomer?.let { "${it.name}" } ?: "नया लेन-देन",
                showBack = true,
                onBackClick = onNavigateBack,
                onHomeClick = onNavigateHome,
                containerColor = TopBarLightBlueBg,
                contentColor = TopBarLightBlueContent,
                borderColor = TopBarLightBlueBorder,
                accentIconTint = TopBarLightBlueAccent
            )
        },
        containerColor = WarmCanvasBg
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. CUSTOMER SELECTOR / DISPLAY
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ग्राहक चुनें",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box {
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { customerDropdownExpanded = true }
                                    .testTag("entry_customer_picker")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = LedgerRedPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (currentCustomer != null) {
                                            Text(
                                                text = "${currentCustomer.name} (${currentCustomer.customerCode})",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDarkPrimary
                                                )
                                            )
                                        } else {
                                            Text(
                                                text = "कोई ग्राहक चुनें...",
                                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = customerDropdownExpanded,
                                onDismissRequest = { customerDropdownExpanded = false }
                            ) {
                                allCustomers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${cust.name} (${cust.customerCode})",
                                                fontWeight = if (cust.id == selectedCustomerId) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedCustomerId = cust.id
                                            customerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. TRANSACTION TYPE SELECTOR TOGGLE (Udhar vs Jama)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Udhar Button
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCredit) UdharRedBg else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isCredit) 2.dp else 1.dp,
                            color = if (isCredit) UdharRed else BorderLight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { transactionType = Transaction.TYPE_CREDIT }
                            .testTag("entry_toggle_udhar")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "उधार (Debit)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCredit) UdharRed else TextDarkSecondary
                                )
                            )
                            // Text(
                            //     text = "ग्राहक से लेना है (-)",
                            //     style = MaterialTheme.typography.labelSmall.copy(
                            //         color = if (isCredit) UdharRed else TextMuted
                            //     )
                            // )
                        }
                    }

                    // Jama Button
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isCredit) JamaGreenBg else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (!isCredit) 2.dp else 1.dp,
                            color = if (!isCredit) JamaGreen else BorderLight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { transactionType = Transaction.TYPE_PAYMENT }
                            .testTag("entry_toggle_jama")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "जमा (Payment)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isCredit) JamaGreen else TextDarkSecondary
                                )
                            )
                            // Text(
                            //     text = "भुगतान प्राप्त हुआ (+)",
                            //     style = MaterialTheme.typography.labelSmall.copy(
                            //         color = if (!isCredit) JamaGreen else TextMuted
                            //     )
                            // )
                        }
                    }
                }
            }

            // 3. AMOUNT DISPLAY & EXPRESSION FIELD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "राशि",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                            )
                            Text(
                                text = "गणना हेतु कैलकुलेटर का उपयोग करें",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Expandable multi-line high-contrast amount box
                        Surface(
                            color = activeBg,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, activeColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "₹",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = activeColor
                                    ),
                                    modifier = Modifier.padding(top = 1.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = if (amountExpression.isEmpty()) "0" else amountExpression,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = activeColor,
                                            textAlign = TextAlign.End,
                                            fontSize = when {
                                                amountExpression.length > 25 -> 18.sp
                                                amountExpression.length > 15 -> 21.sp
                                                amountExpression.length > 10 -> 24.sp
                                                else -> 26.sp
                                            },
                                            lineHeight = when {
                                                amountExpression.length > 25 -> 24.sp
                                                amountExpression.length > 15 -> 27.sp
                                                amountExpression.length > 10 -> 30.sp
                                                else -> 32.sp
                                            }
                                        ),
                                        softWrap = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("entry_amount_display")
                                    )

                                    // Real-time live evaluated calculation preview (always displayed in grey to prevent layout height shifting)
                                    val previewText = remember(amountExpression) {
                                        if (amountExpression.isBlank()) {
                                            "= ₹0"
                                        } else {
                                            val evaluated = CalculatorEngine.evaluate(amountExpression)
                                            if (evaluated != null) {
                                                "= ₹$evaluated"
                                            } else {
                                                val cleanPrefix = amountExpression.trimEnd('+', '-', '×', '÷', '*', '/')
                                                val prefixEval = CalculatorEngine.evaluate(cleanPrefix)
                                                if (prefixEval != null) "= ₹$prefixEval" else "= ₹..."
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = previewText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF64748B),
                                            textAlign = TextAlign.End,
                                            fontSize = 14.sp
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("entry_amount_preview")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 4. INTERACTIVE ARITHMETIC CALCULATOR KEYPAD
                        InteractiveCalculatorKeypad(
                            onKeyPress = { key ->
                                when (key) {
                                    "C" -> amountExpression = ""
                                    "DEL" -> if (amountExpression.isNotEmpty()) {
                                        amountExpression = amountExpression.dropLast(1)
                                    }
                                    "=" -> {
                                        val evaluated = CalculatorEngine.evaluate(amountExpression)
                                        if (evaluated != null) {
                                            amountExpression = evaluated
                                        }
                                    }
                                    else -> {
                                        amountExpression += key
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 5. DESCRIPTION & DATE/TIME SECTION
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Description Field with multi-line expandability and adaptive text scaling
                        val descFontSize = when {
                            description.length > 80 || description.count { it == '\n' } >= 3 -> 12.sp
                            description.length > 35 || description.count { it == '\n' } >= 1 -> 13.5.sp
                            else -> 15.sp
                        }
                        val descLineHeight = when {
                            description.length > 80 || description.count { it == '\n' } >= 3 -> 16.sp
                            description.length > 35 || description.count { it == '\n' } >= 1 -> 18.sp
                            else -> 20.sp
                        }

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("विवरण (सामान का नाम या टिप्पणी)") },
                            // placeholder = { Text("उदा. 5 किलो आटा, तेल, पुराना हिसाब आदि") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextMuted) },
                            singleLine = false,
                            minLines = 1,
                            maxLines = 5,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = descFontSize,
                                lineHeight = descLineHeight,
                                color = TextDarkPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LedgerRedPrimary,
                                unfocusedBorderColor = BorderLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("entry_description_input")
                        )

                        // Date & Time pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date Picker
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    val parts = entryDate.split("-")
                                    val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
                                    val m = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
                                    val d = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

                                    val picker = DatePickerDialog(context, { _, year, month, dayOfMonth ->
                                        entryDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    }, y, m, d)
                                    picker.datePicker.maxDate = System.currentTimeMillis()
                                    picker.show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("entry_date_picker_button")
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = DateTimeUtils.formatToHindiDate(entryDate),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Time Picker
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    val parts = entryTime.split(":")
                                    val h = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
                                    val min = parts.getOrNull(1)?.toIntOrNull() ?: cal.get(Calendar.MINUTE)

                                    TimePickerDialog(context, { _, hourOfDay, minute ->
                                        entryTime = String.format("%02d:%02d:00", hourOfDay, minute)
                                    }, h, min, false).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("entry_time_picker_button")
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = DateTimeUtils.formatTo12HourTime(entryTime),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 6. SAVE BUTTON
            item {
                Button(
                    onClick = {
                        if (currentCustomer == null) {
                            return@Button
                        }
                        viewModel.addTransaction(
                            customerId = currentCustomer.id,
                            type = transactionType,
                            amountInput = amountExpression,
                            description = description,
                            entryDate = entryDate,
                            entryTime = entryTime,
                            onSuccess = { savedTxn ->
                                confirmedTransaction = Pair(currentCustomer, savedTxn)
                            }
                        )
                    },
                    enabled = currentCustomer != null && amountExpression.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCredit) UdharRed else JamaGreen
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("entry_submit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCredit) "उधार रिकॉर्ड करें" else "जमा रिकॉर्ड करें",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }

    // Confirmation Dialog
    if (confirmedTransaction != null) {
        val (cust, txn) = confirmedTransaction!!
        TransactionConfirmationDialog(
            customer = cust,
            transaction = txn,
            onDismiss = {
                confirmedTransaction = null
                onNavigateBack()
            },
            onViewCustomerLedger = {
                confirmedTransaction = null
                onNavigateToCustomerDetail(cust.id)
            }
        )
    }
}

@Composable
private fun InteractiveCalculatorKeypad(
    onKeyPress: (String) -> Unit
) {
    val rows = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf("C", "0", ".", "+"),
        listOf("DEL", "=")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.take(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { key ->
                    val isOperator = key in listOf("+", "-", "×", "÷")
                    val isClear = key == "C"

                    Button(
                        onClick = { onKeyPress(key) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isClear -> Color(0xFFFEE2E2)
                                isOperator -> Color(0xFFFEF3C7)
                                else -> Color(0xFFF1F5F9)
                            },
                            contentColor = when {
                                isClear -> UdharRed
                                isOperator -> Color(0xFFB45309)
                                else -> Color(0xFF1E293B)
                            }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("calc_key_$key")
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                    }
                }
            }
        }

        // Bottom row for DEL and =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Delete Key
            Button(
                onClick = { onKeyPress("DEL") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE2E8F0),
                    contentColor = Color(0xFF475569)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("calc_key_del")
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "मिटाएं",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Equal Key
            Button(
                onClick = { onKeyPress("=") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaffronGold,
                    contentColor = Color(0xFF451A03)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("calc_key_equal")
            ) {
                Text(
                    text = "= गणना करें",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}
