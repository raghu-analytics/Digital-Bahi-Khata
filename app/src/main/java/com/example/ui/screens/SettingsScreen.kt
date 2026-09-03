package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToShopDetails: () -> Unit,
    onNavigateToDataManagement: () -> Unit,
    onNavigateToHelpFaq: () -> Unit,
    onNavigateToAppSettings: () -> Unit,
    onNavigateToMasterInfo: () -> Unit
) {
    Scaffold(
        topBar = {
            BahiKhataTopBar(
                title = "सेटिंग्स",
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Option 1: Shop & Owner Details
            item {
                SettingsMenuCard(
                    icon = Icons.Default.Storefront,
                    iconTint = LedgerRedPrimary,
                    iconBg = Color(0xFFFEE2E2),
                    title = "1. दुकान एवं मालिक का विवरण",
                    subtitle = "दुकान का नाम, मालिक, मोबाइल नंबर व पता बदलें",
                    testTag = "settings_menu_shop_details",
                    onClick = onNavigateToShopDetails
                )
            }

            // Option 2: Data Management
            item {
                SettingsMenuCard(
                    icon = Icons.Default.Storage,
                    iconTint = JamaGreen,
                    iconBg = Color(0xFFDCFCE7),
                    title = "2. डेटा प्रबंधन (Data Management)",
                    subtitle = "बैकअप, रीस्टोर, एक्सेल एक्सपोर्ट व डेटा रीसेट",
                    testTag = "settings_menu_data_management",
                    onClick = onNavigateToDataManagement
                )
            }

            // Option 3: Help & FAQs
            item {
                SettingsMenuCard(
                    icon = Icons.Default.HelpOutline,
                    iconTint = Color(0xFF0284C7),
                    iconBg = Color(0xFFE0F2FE),
                    title = "3. मदद एवं सामान्य प्रश्न",
                    subtitle = "ऐप उपयोग की जानकारी",
                    testTag = "settings_menu_help_faq",
                    onClick = onNavigateToHelpFaq
                )
            }

            // Option 4: App Settings
            item {
                SettingsMenuCard(
                    icon = Icons.Default.Tune,
                    iconTint = Color(0xFF7E22CE),
                    iconBg = Color(0xFFF3E8FF),
                    title = "4. ऐप सेटिंग्स",
                    subtitle = "थीम, भाषा इत्यादि",
                    testTag = "settings_menu_app_settings",
                    onClick = onNavigateToAppSettings
                )
            }

            // Option 5: Master Info
            item {
                SettingsMenuCard(
                    icon = Icons.Default.Dataset,
                    iconTint = Color(0xFFD97706),
                    iconBg = Color(0xFFFEF3C7),
                    title = "5. Master Info",
                    subtitle = "डेटाबेस की तकनीकी जानकारी",
                    testTag = "settings_menu_master_info",
                    onClick = onNavigateToMasterInfo
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = iconBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextDarkSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
