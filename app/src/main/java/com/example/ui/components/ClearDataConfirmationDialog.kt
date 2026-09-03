package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.UdharRed

@Composable
fun ClearDataConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirmClear: () -> Unit
) {
    var confirmationInput by remember { mutableStateOf("") }
    val isValid = confirmationInput.trim().equals("DELETE", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = UdharRed,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "सभी डेटा मिटाएं?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = UdharRed
                    )
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "चेतावनी: यह क्रिया सभी ग्राहकों और उनके संपूर्ण लेन-देन के इतिहास को पूरी तरह से हटा देगी। यह वापस नहीं किया जा सकता!",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF334155))
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "पुष्टि के लिए नीचे 'DELETE' टाइप करें:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmationInput,
                    onValueChange = { confirmationInput = it },
                    placeholder = { Text("DELETE") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_data_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirmClear()
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = UdharRed),
                modifier = Modifier.testTag("clear_data_confirm_button")
            ) {
                Text("डेटा मिटाएं (Delete All)")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("clear_data_cancel_button")
            ) {
                Text("रद्द करें")
            }
        }
    )
}
