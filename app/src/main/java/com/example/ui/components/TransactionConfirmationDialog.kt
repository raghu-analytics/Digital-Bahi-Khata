package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.util.CurrencyUtils
import com.example.util.DateTimeUtils
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.JamaGreenBg
import com.example.ui.theme.LedgerRedPrimary
import com.example.ui.theme.UdharRed
import com.example.ui.theme.UdharRedBg

@Composable
fun TransactionConfirmationDialog(
    customer: Customer,
    transaction: Transaction,
    onDismiss: () -> Unit,
    onViewCustomerLedger: (() -> Unit)? = null
) {
    val isCredit = transaction.isCredit
    val badgeBg = if (isCredit) UdharRedBg else JamaGreenBg
    val badgeColor = if (isCredit) UdharRed else JamaGreen
    val typeText = if (isCredit) "उधार (Debit)" else "जमा (Payment/Credit)"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(JamaGreenBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = JamaGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Entry Saved",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    )
                    Text(
                        text = "बही-खाते में दर्ज हो चुका है",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }
            }
        },
        text = {
            Card(
                colors = CardDefaults.cardColors(containerColor = badgeBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Type & Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = typeText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        )
                        Text(
                            text = CurrencyUtils.formatPaiseToRupees(transaction.amountPaise),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeColor
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(badgeColor.copy(alpha = 0.2f))
                    )

                    // Customer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "ग्राहक:", color = Color.DarkGray, fontSize = 13.sp)
                        Text(
                            text = "${customer.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Date & Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "दिनांक व समय:", color = Color.DarkGray, fontSize = 13.sp)
                        Text(
                            text = "${DateTimeUtils.formatToHindiDate(transaction.entryDate)}, ${DateTimeUtils.formatTo12HourTime(transaction.entryTime)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Description if present
                    if (!transaction.description.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "विवरण:", color = Color.DarkGray, fontSize = 13.sp)
                            Text(
                                text = transaction.description,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = LedgerRedPrimary),
                modifier = Modifier.testTag("confirm_dialog_ok_button")
            ) {
                Text("  OK  ")
            }
        },
        dismissButton = {
            if (onViewCustomerLedger != null) {
                OutlinedButton(
                    onClick = {
                        onViewCustomerLedger()
                    },
                    modifier = Modifier.testTag("confirm_dialog_view_ledger_button")
                ) {
                    Text("खाता देखें")
                }
            }
        }
    )
}
