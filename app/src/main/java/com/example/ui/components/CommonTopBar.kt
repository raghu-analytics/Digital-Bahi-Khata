package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color definitions for light top bars
val TopBarLightRedBg = Color(0xFFFEE2E2)
val TopBarLightRedContent = Color(0xFF991B1B)
val TopBarLightRedBorder = Color(0xFFFECACA)
val TopBarLightRedAccent = Color(0xFFDC2626)

val TopBarLightOrangeBg = Color(0xFFFFEDD5)
val TopBarLightOrangeContent = Color(0xFF9A3412)
val TopBarLightOrangeBorder = Color(0xFFFED7AA)
val TopBarLightOrangeAccent = Color(0xFFEA580C)

val TopBarLightPurpleBg = Color(0xFFF3E8FF)
val TopBarLightPurpleContent = Color(0xFF581C87)
val TopBarLightPurpleBorder = Color(0xFFE9D5FF)
val TopBarLightPurpleAccent = Color(0xFF9333EA)

val TopBarLightBlueBg = Color(0xFFE0F2FE)
val TopBarLightBlueContent = Color(0xFF0369A1)
val TopBarLightBlueBorder = Color(0xFFBAE6FD)
val TopBarLightBlueAccent = Color(0xFF0284C7)

val TopBarLightYellowBg = Color(0xFFFEF9C3)
val TopBarLightYellowContent = Color(0xFF78350F)
val TopBarLightYellowBorder = Color(0xFFFEF08A)
val TopBarLightYellowAccent = Color(0xFFD97706)

val TopBarLightGreyBg = Color(0xFFF1F5F9)
val TopBarLightGreyContent = Color(0xFF334155)
val TopBarLightGreyBorder = Color(0xFFCBD5E1)
val TopBarLightGreyAccent = Color(0xFF475569)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BahiKhataTopBar(
    title: String = "डिजिटल बही-खाता",
    subtitle: String? = "दुकानदार का डिजिटल खाता",
    showBack: Boolean = false,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onReportsClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
    containerColor: Color = TopBarLightRedBg,
    contentColor: Color = TopBarLightRedContent,
    iconTint: Color = contentColor,
    accentIconTint: Color = contentColor,
    borderColor: Color = TopBarLightRedBorder,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = containerColor,
        shadowElevation = 2.dp
    ) {
        Column {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "बही-खाता",
                                tint = accentIconTint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(end = 6.dp)
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = contentColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = contentColor.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("nav_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "पीछे जाएं",
                                tint = iconTint
                            )
                        }
                    }
                },
                actions = {
                    actions()

                    if (onReportsClick != null) {
                        IconButton(
                            onClick = onReportsClick,
                            modifier = Modifier.testTag("nav_reports_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "मासिक रिपोर्ट",
                                tint = iconTint
                            )
                        }
                    }

                    if (showBack) {
                        IconButton(
                            onClick = onHomeClick,
                            modifier = Modifier.testTag("nav_home_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "होम स्क्रीन",
                                tint = iconTint
                            )
                        }
                    }

                    if (onHelpClick != null) {
                        IconButton(
                            onClick = onHelpClick,
                            modifier = Modifier.testTag("nav_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "मदद एवं सहायता",
                                tint = iconTint
                            )
                        }
                    }

                    if (onSettingsClick != null) {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.testTag("nav_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "सेटिंग्स",
                                tint = iconTint
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    titleContentColor = contentColor,
                    navigationIconContentColor = iconTint,
                    actionIconContentColor = iconTint
                )
            )

            // Subtle bottom trim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(borderColor)
                    .size(width = 0.dp, height = 2.dp)
            )
        }
    }
}

