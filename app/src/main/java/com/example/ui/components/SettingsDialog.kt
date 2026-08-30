package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LyraBorderGlow
import com.example.ui.theme.LyraCyan
import com.example.ui.theme.LyraSurfaceCard
import com.example.ui.theme.LyraSurfaceDark
import com.example.ui.theme.LyraViolet

@Composable
fun SettingsDialog(
    currentApiKey: String,
    onSaveApiKey: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var voicePitch by remember { mutableFloatStateOf(1.18f) }
    var voiceRate by remember { mutableFloatStateOf(1.03f) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val closeAndDismiss = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    Dialog(onDismissRequest = closeAndDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, LyraBorderGlow, RoundedCornerShape(20.dp))
                .testTag("settings_dialog"),
            colors = CardDefaults.cardColors(containerColor = LyraSurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = LyraCyan, modifier = Modifier.size(22.dp))
                        Text(
                            text = "LYRA System Configuration",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gemini API Configuration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = LyraViolet, modifier = Modifier.size(16.dp))
                    Text("Gemini API Key Override", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LyraCyan)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = { Text("Enter custom Gemini API Key (optional)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LyraCyan,
                        unfocusedBorderColor = LyraBorderGlow
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Voice Tuning
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = LyraCyan, modifier = Modifier.size(16.dp))
                    Text("Voice Assistant Acoustics", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LyraCyan)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text("Pitch: ${String.format("%.2f", voicePitch)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = voicePitch,
                    onValueChange = { voicePitch = it },
                    valueRange = 0.8f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = LyraCyan, activeTrackColor = LyraCyan),
                    modifier = Modifier.testTag("pitch_slider")
                )

                Text("Speech Rate: ${String.format("%.2f", voiceRate)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = voiceRate,
                    onValueChange = { voiceRate = it },
                    valueRange = 0.8f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = LyraViolet, activeTrackColor = LyraViolet),
                    modifier = Modifier.testTag("rate_slider")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Privacy Shield Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LyraSurfaceCard)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = LyraCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Data Retention: Strict zero permanent screenshot storage. Ephemeral frame cache only.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onSaveApiKey(apiKeyInput)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LyraCyan),
                    modifier = Modifier.fillMaxWidth().testTag("save_settings_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply & Save Settings", color = LyraSurfaceDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
