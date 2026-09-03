package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BahiKhataTopBar
import com.example.ui.components.TopBarLightYellowAccent
import com.example.ui.components.TopBarLightYellowBg
import com.example.ui.components.TopBarLightYellowBorder
import com.example.ui.components.TopBarLightYellowContent
import com.example.ui.theme.BorderLight
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.LedgerRedPrimary
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarmCanvasBg
import com.example.ui.viewmodel.BahiKhataViewModel
import java.io.File
import java.util.Locale

@Composable
fun MasterInfoScreen(
    viewModel: BahiKhataViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val shopSettings by viewModel.shopSettings.collectAsStateWithLifecycle()

    val dbFile = context.getDatabasePath("bahi_khata_database.db")
    val dbExists = dbFile.exists()
    val dbSizeBytes = if (dbExists) dbFile.length() else 0L
    val dbSizeFormatted = formatFileSize(dbSizeBytes)

    Scaffold(
        topBar = {
            BahiKhataTopBar(
                title = "Master Info",
                subtitle = "डेटाबेस एवं सिस्टम की सम्पूर्ण तकनीकी जानकारी",
                showBack = true,
                onBackClick = onNavigateBack,
                onHomeClick = onNavigateHome,
                containerColor = TopBarLightYellowBg,
                contentColor = TopBarLightYellowContent,
                borderColor = TopBarLightYellowBorder,
                accentIconTint = TopBarLightYellowAccent
            )
        },
        containerColor = WarmCanvasBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Overview Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFD97706),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Dataset,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "डेटाबेस लाइव स्थिति",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            )
                            // Text(
                            //     text = "बही-खाता का सम्पूर्ण डेटा डिवाइस पर स्थानीय रूप से सुरक्षित है।",
                            //     style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB45309))
                            // )
                        }
                    }
                }
            }

            // Database Details Card (Row by Row)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "डेटाबेस तकनीकी विवरण ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Row 1: Database Name
                        MasterInfoRow(
                            icon = Icons.Default.Storage,
                            iconTint = LedgerRedPrimary,
                            label = "Database Name",
                            value = "bahi_khata_database.db"
                        )

                        // Row 2: Database Path
                        MasterInfoRow(
                            icon = Icons.Default.Folder,
                            iconTint = Color(0xFF0284C7),
                            label = "Database Path",
                            value = dbFile.absolutePath,
                            isMonospace = true
                        )

                        // Row 3: Database Size
                        MasterInfoRow(
                            icon = Icons.Default.DataObject,
                            iconTint = Color(0xFFD97706),
                            label = "Database File Size",
                            value = "$dbSizeFormatted "
                        )

                        // Row 4: Total Customers
                        MasterInfoRow(
                            icon = Icons.Default.People,
                            iconTint = JamaGreen,
                            label = "Total Customers",
                            value = "${customers.size} ग्राहक"
                        )

                        // Row 5: Total Transactions
                        MasterInfoRow(
                            icon = Icons.Default.ReceiptLong,
                            iconTint = Color(0xFF8B5CF6),
                            label = "Total Transactions",
                            value = "${transactions.size} प्रविष्टियां"
                        )

                        // Row 6: Total App Settings
                        MasterInfoRow(
                            icon = Icons.Default.Settings,
                            iconTint = Color(0xFFEC4899),
                            label = "Configured Settings",
                            value = "${shopSettings.size} पैरामीटर"
                        )

                        // Row 7: Architecture / Mode
                        MasterInfoRow(
                            icon = Icons.Default.Security,
                            iconTint = JamaGreen,
                            label = "Storage Architecture",
                            value = "100% ऑफ़लाइन (Room SQLite Local)"
                        )

                        // Row 8: Version
                        MasterInfoRow(
                            icon = Icons.Default.Info,
                            iconTint = Color(0xFF475569),
                            label = "Schema Version",
                            value = "Room SQLite Version 1"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterInfoRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary,
                fontSize = if (isMonospace) 12.sp else 14.sp,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            ),
            modifier = Modifier.padding(start = 22.dp)
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.2f MB", mb)
    } else {
        String.format(Locale.US, "%.1f KB", kb)
    }
}
