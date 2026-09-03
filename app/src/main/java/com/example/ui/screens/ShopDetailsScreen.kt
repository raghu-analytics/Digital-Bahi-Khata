package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppSetting
import com.example.ui.components.BahiKhataTopBar
import com.example.ui.components.TopBarLightYellowAccent
import com.example.ui.components.TopBarLightYellowBg
import com.example.ui.components.TopBarLightYellowBorder
import com.example.ui.components.TopBarLightYellowContent
import com.example.ui.theme.BorderLight
import com.example.ui.theme.LedgerRedPrimary
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarmCanvasBg
import com.example.ui.viewmodel.BahiKhataViewModel

@Composable
fun ShopDetailsScreen(
    viewModel: BahiKhataViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val shopSettings by viewModel.shopSettings.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(shopSettings) {
        if (!isInitialized && shopSettings.isNotEmpty()) {
            shopName = shopSettings[AppSetting.KEY_SHOP_NAME] ?: ""
            ownerName = shopSettings[AppSetting.KEY_OWNER_NAME] ?: ""
            address = shopSettings[AppSetting.KEY_ADDRESS] ?: ""
            mobile = shopSettings[AppSetting.KEY_MOBILE] ?: ""
            isInitialized = true
        }
    }

    Scaffold(
        topBar = {
            BahiKhataTopBar(
                title = "दुकान विवरण",
                subtitle = "प्रिंट पर्ची एवं रिपोर्ट पर दिखने वाला विवरण",
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
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Edit Form Card
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = LedgerRedPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "दुकान की जानकारी",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                            )
                        }

                        Text(
                            text = "यह जानकारी आपकी सभी PDF रिपोर्टों और खाता पर्चियों के शीर्ष पर प्रदर्शित होगी।",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextDarkSecondary)
                        )

                        // Shop Name
                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text("दुकान / व्यापार का नाम") },
                            // placeholder = { Text("उदा. गुप्ता किराना स्टोर") },
                            leadingIcon = {
                                Icon(Icons.Default.Business, contentDescription = null, tint = LedgerRedPrimary)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LedgerRedPrimary,
                                focusedLabelColor = LedgerRedPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_name_input")
                        )

                        // Owner Name
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("दुकानदार का नाम") },
                            // placeholder = { Text("उदा. रमेश गुप्ता") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = LedgerRedPrimary)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LedgerRedPrimary,
                                focusedLabelColor = LedgerRedPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_name_input")
                        )

                        // Mobile Number
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { if (it.length <= 15) mobile = it },
                            label = { Text("संपर्क मोबाइल नंबर") },
                            // placeholder = { Text("उदा. 9876543210") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = LedgerRedPrimary)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LedgerRedPrimary,
                                focusedLabelColor = LedgerRedPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_mobile_input")
                        )

                        // Address
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("दुकान का पता") },
                            // placeholder = { Text("उदा. मुख्य बाजार, स्टेशन रोड") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = LedgerRedPrimary)
                            },
                            minLines = 2,
                            maxLines = 4,
                            singleLine = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LedgerRedPrimary,
                                focusedLabelColor = LedgerRedPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_address_input")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Save Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.saveShopSettings(
                                    shopName = shopName,
                                    ownerName = ownerName,
                                    address = address,
                                    mobile = mobile
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LedgerRedPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("save_shop_settings_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "विवरण सुरक्षित करें (Save Details)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // 2. Live Print Preview Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5E4)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "प्रिंट पर्ची पूर्वावलोकन (Preview):",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = shopName.ifBlank { "डिजिटल बही-खाता" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = LedgerRedPrimary
                            )
                        )
                        if (ownerName.isNotBlank() || mobile.isNotBlank() || address.isNotBlank()) {
                            val detailsList = mutableListOf<String>()
                            if (ownerName.isNotBlank()) detailsList.add("दुकानदार : $ownerName")
                            if (mobile.isNotBlank()) detailsList.add("मो: $mobile")
                            if (address.isNotBlank()) detailsList.add("पता: $address")
                            Text(
                                text = detailsList.joinToString(" | "),
                                style = MaterialTheme.typography.bodySmall.copy(color = TextDarkSecondary)
                            )
                        }
                    }
                }
            }
        }
    }
}
