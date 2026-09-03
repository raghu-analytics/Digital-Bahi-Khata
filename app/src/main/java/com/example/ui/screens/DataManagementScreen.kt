package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BahiKhataTopBar
import com.example.ui.components.ClearDataConfirmationDialog
import com.example.ui.components.TopBarLightYellowAccent
import com.example.ui.components.TopBarLightYellowBg
import com.example.ui.components.TopBarLightYellowBorder
import com.example.ui.components.TopBarLightYellowContent
import com.example.ui.theme.BorderLight
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.LedgerRedPrimary
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.UdharRed
import com.example.ui.theme.WarmCanvasBg
import com.example.ui.viewmodel.BahiKhataViewModel

@Composable
fun DataManagementScreen(
    viewModel: BahiKhataViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val restoreValidationMessage by viewModel.restoreValidationMessage.collectAsStateWithLifecycle()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showExportDbConfirm by remember { mutableStateOf(false) }
    var showExportExcelConfirm by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    // File picker launcher for SQLite DB restore
    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreBackup(uri)
        }
    }

    Scaffold(
        topBar = {
            BahiKhataTopBar(
                title = "डेटा प्रबंधन (Data Management)",
                subtitle = "बैकअप, रीस्टोर एवं डेटा सुरक्षा",
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
            // Header Info Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "100% सुरक्षित एवं ऑफ़लाइन बैकअप",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                            )
                            Text(
                                text = "नियमित बैकअप डाउनलोड करके अपने फ़ोन के सुरक्षित फोल्डर या ईमेल में रखें।",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1E40AF))
                            )
                        }
                    }
                }
            }

            // 1. Export SQLite Backup
            item {
                DataActionCard(
                    icon = Icons.Default.Backup,
                    iconTint = JamaGreen,
                    title = "डेटाबेस बैकअप डाउनलोड करें",
                    description = "सभी ग्राहकों व लेन-देन का मूल डेटाबेस (.db फ़ाइल) सीधे Download फोल्डर में सुरक्षित करें।",
                    buttonText = "बैकअप डाउनलोड करें (.db)",
                    buttonColor = JamaGreen,
                    testTag = "export_db_button",
                    onClick = { showExportDbConfirm = true }
                )
            }

            // 2. Restore SQLite Backup
            item {
                DataActionCard(
                    icon = Icons.Default.Restore,
                    iconTint = Color(0xFF0284C7),
                    title = "डेटाबेस बैकअप रीस्टोर करें",
                    description = "पूर्व में डाउनलोड की गई .db बैकअप फ़ाइल से बही-खाता का पूरा डेटा वापस लोड करें।",
                    buttonText = "फ़ाइल चुनें एवं रीस्टोर करें",
                    buttonColor = Color(0xFF0284C7),
                    testTag = "restore_db_button",
                    onClick = { showRestoreConfirm = true }
                )
            }

            // 3. Export Excel (.xlsx)
            item {
                DataActionCard(
                    icon = Icons.Default.TableChart,
                    iconTint = Color(0xFFD97706),
                    title = "एक्सेल (.xlsx) रिपोर्ट डाउनलोड करें",
                    description = "तीनों तालिकाएँ (दुकान विवरण, ग्राहक सूची, लेन-देन) 3 अलग-अलग शीट्स में एक ही Excel (.xlsx) फ़ाइल में प्राप्त करें।",
                    buttonText = "3-शीट्स Excel (.xlsx) डाउनलोड करें",
                    buttonColor = Color(0xFFD97706),
                    testTag = "export_excel_button",
                    onClick = { showExportExcelConfirm = true }
                )
            }

            // 4. Delete All Data
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = UdharRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "संपूर्ण डेटा हटाएं (Delete All Data)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = UdharRed
                                )
                            )
                        }

                        Text(
                            text = "चेतावनी: इससे ऐप का समस्त डेटा (सभी ग्राहक और उनका संपूर्ण लेन-देन इतिहास) स्थायी रूप से हटा दिया जाएगा। कृपया हटाने से पूर्व बैकअप अवश्य ले लें।",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextDarkSecondary)
                        )

                        Button(
                            onClick = { showClearConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = UdharRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("clear_all_data_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "सभी डेटा हमेशा के लिए हटाएं",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Confirmation Dialogs ---

    // 1. Export DB Confirmation
    if (showExportDbConfirm) {
        AlertDialog(
            onDismissRequest = { showExportDbConfirm = false },
            title = {
                Text(
                    text = "डेटाबेस बैकअप डाउनलोड करें?",
                    fontWeight = FontWeight.Bold,
                    color = LedgerRedPrimary
                )
            },
            text = {
                Text(
                    text = "क्या आप वर्तमान बही-खाता का पूर्ण बैकअप (.db फ़ाइल) अपने Download फोल्डर में सुरक्षित करना चाहते हैं?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextDarkPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDbConfirm = false
                        viewModel.createBackup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JamaGreen),
                    modifier = Modifier.testTag("confirm_export_db")
                ) {
                    Text("हां, बैकअप लें")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportDbConfirm = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // 2. Restore DB Initial Warning Dialog
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = {
                Text(
                    text = "बैकअप रीस्टोर चेतावनी",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706)
                )
            },
            text = {
                Text(
                    text = "बैकअप रीस्टोर करने पर वर्तमान डेटाबेस चुनी गई फ़ाइल से बदल दिया जाएगा। आगे बढ़ने के लिए अपनी बैकअप (.db) फ़ाइल चुनें।",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextDarkPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        restoreFilePicker.launch("*/*")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.testTag("confirm_open_file_picker")
                ) {
                    Text("फ़ाइल चुनें")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreConfirm = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // 3. Restore Validation & Final Execution Dialog
    if (restoreValidationMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelRestorePreview() },
            title = {
                Text(
                    text = "बैकअप डेटाबेस विवरण",
                    fontWeight = FontWeight.Bold,
                    color = LedgerRedPrimary
                )
            },
            text = {
                Text(
                    text = restoreValidationMessage!!,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextDarkPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmRestore()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JamaGreen),
                    modifier = Modifier.testTag("confirm_execute_restore")
                ) {
                    Text("हां, रीस्टोर करें")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cancelRestorePreview() }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // 4. Export Excel Confirmation
    if (showExportExcelConfirm) {
        AlertDialog(
            onDismissRequest = { showExportExcelConfirm = false },
            title = {
                Text(
                    text = "3-शीट्स Excel (.xlsx) फ़ाइल डाउनलोड करें?",
                    fontWeight = FontWeight.Bold,
                    color = LedgerRedPrimary
                )
            },
            text = {
                Text(
                    text = "क्या आप सभी 3 तालिकाओं (दुकान विवरण, ग्राहक सूची, लेन-देन) को 3 अलग-अलग शीट्स वाली एकल Excel (.xlsx) फ़ाइल में डाउनलोड करना चाहते हैं?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextDarkPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportExcelConfirm = false
                        viewModel.exportToExcelCsv()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    modifier = Modifier.testTag("confirm_export_excel")
                ) {
                    Text("हां, डाउनलोड करें")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportExcelConfirm = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // 5. Delete All Data Dialog
    if (showClearConfirmDialog) {
        ClearDataConfirmationDialog(
            onDismiss = { showClearConfirmDialog = false },
            onConfirmClear = {
                viewModel.clearAllData()
                showClearConfirmDialog = false
            }
        )
    }
}

@Composable
private fun DataActionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    buttonText: String,
    buttonColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(color = TextDarkSecondary)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag(testTag)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
