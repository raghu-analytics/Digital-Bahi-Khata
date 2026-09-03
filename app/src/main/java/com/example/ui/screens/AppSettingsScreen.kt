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
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun AppSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    Scaffold(
        topBar = {
            BahiKhataTopBar(
                title = "ऐप सेटिंग्स ",
                subtitle = "सिस्टम एवं प्रदर्शन प्राथमिकताएं",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Notice Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF7E22CE),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "आगामी संस्करण (Coming Soon)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF581C87)
                                )
                            )
                            Text(
                                text = "ऐप सेटिंग्स एवं अतिरिक्त कस्टमाइज़ेशन विकल्प आगामी संस्करण में जोड़े जाएंगे।",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B21A8))
                            )
                        }
                    }
                }
            }

            // Future Preview Options
            item {
                Text(
                    text = "नियोजित सुविधाएं (Planned Options):",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary
                    ),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            item {
                AppSettingPlaceholderCard(
                    icon = Icons.Default.ColorLens,
                    iconTint = Color(0xFF3B82F6),
                    title = "ऐप थीम ",
                    status = "क्लासिक लाल बही-खाता (डिफ़ॉल्ट)"
                )
            }

            item {
                AppSettingPlaceholderCard(
                    icon = Icons.Default.Language,
                    iconTint = Color(0xFF10B981),
                    title = "भाषा (Language)",
                    status = "हिंदी (Hindi)"
                )
            }

            item {
                AppSettingPlaceholderCard(
                    icon = Icons.Default.Notifications,
                    iconTint = Color(0xFFF59E0B),
                    title = "दैनिक हिसाब (Reminders)",
                    status = "स्वचालित"
                )
            }
        }
    }
}

@Composable
private fun AppSettingPlaceholderCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    status: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = iconTint.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }

            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "जल्द उपलब्ध",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
