package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.UdharRed
import com.example.ui.theme.WarmCanvasBg
import com.example.ui.viewmodel.BahiKhataViewModel

@Composable
fun HelpFaqScreen(
    viewModel: BahiKhataViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val faqText = remember { viewModel.loadFaqContent() }

    Scaffold(
        topBar = {
            BahiKhataTopBar(
                title = "मदद एवं सामान्य प्रश्न",
                subtitle = "सहायता और उपयोगकर्ता गाइड",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Highlights header card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = SaffronGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "100% सुरक्षित और ऑफलाइन",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "डिजिटल बही-खाता पूरी तरह से आपके मोबाइल में चलता है। आपका डेटा किसी भी बाहरी सर्वर पर नहीं जाता।",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextDarkSecondary)
                        )
                    }
                }
            }

            // FAQ Content Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("faq_content_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "सहायता नियमावली (Help Guide)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = LedgerRedPrimary
                            )
                        )

                        // Render each line or section cleanly
                        val lines = faqText.lines()
                        lines.forEach { line ->
                            when {
                                line.startsWith("## ") -> {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = line.removePrefix("## "),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextDarkPrimary,
                                            fontSize = 15.sp
                                        )
                                    )
                                }
                                line.startsWith("# ") -> {
                                    Text(
                                        text = line.removePrefix("# "),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = LedgerRedPrimary
                                        )
                                    )
                                }
                                line.startsWith("- ") -> {
                                    Row(
                                        modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(text = "• ", fontWeight = FontWeight.Bold, color = LedgerRedPrimary)
                                        Text(
                                            text = line.removePrefix("- "),
                                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF334155))
                                        )
                                    }
                                }
                                line.isNotBlank() -> {
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFF334155),
                                            lineHeight = 20.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
