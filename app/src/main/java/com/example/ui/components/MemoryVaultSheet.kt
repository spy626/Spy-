package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.memory.MemoryCategory
import com.example.memory.MemoryEntity
import com.example.ui.theme.LyraBorderGlow
import com.example.ui.theme.LyraCyan
import com.example.ui.theme.LyraError
import com.example.ui.theme.LyraSuccess
import com.example.ui.theme.LyraSurfaceCard
import com.example.ui.theme.LyraSurfaceDark
import com.example.ui.theme.LyraViolet
import com.example.ui.theme.LyraWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryVaultSheet(
    memories: List<MemoryEntity>,
    onDismiss: () -> Unit,
    onSaveMemory: (key: String, value: String, category: MemoryCategory) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onQuickTestCorrection: () -> Unit,
    onQuickTestSensitiveShield: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var showAddDialog by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    val closeSheet = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = closeSheet,
        sheetState = sheetState,
        containerColor = LyraSurfaceDark,
        modifier = modifier.testTag("memory_vault_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
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
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = LyraViolet,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "LYRA Memory Vault",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = { showAddDialog = !showAddDialog },
                    colors = ButtonDefaults.buttonColors(containerColor = LyraViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_memory_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Memory", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Safety Shield Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1B4B))
                    .border(1.dp, LyraViolet.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = LyraCyan, modifier = Modifier.size(20.dp))
                Column {
                    Text(
                        text = "Encrypted On-Device Privacy Shield",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LyraCyan
                    )
                    Text(
                        text = "Passwords, OTPs, and banking credentials are automatically blocked from memory storage.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Test Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickTestCorrection,
                    colors = ButtonDefaults.buttonColors(containerColor = LyraSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LyraBorderGlow),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("test_correction_btn")
                ) {
                    Text("Test Correction\n(Kareem → Karima)", fontSize = 10.sp, color = LyraCyan)
                }

                Button(
                    onClick = onQuickTestSensitiveShield,
                    colors = ButtonDefaults.buttonColors(containerColor = LyraSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LyraBorderGlow),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("test_sensitive_shield_btn")
                ) {
                    Text("Test Privacy\n(Block Password/OTP)", fontSize = 10.sp, color = LyraWarning)
                }
            }

            if (showAddDialog) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(LyraSurfaceCard)
                        .padding(12.dp)
                ) {
                    Text("Store New Knowledge / Memory", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LyraCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Memory Key (e.g. friend_name)") },
                        modifier = Modifier.fillMaxWidth().testTag("memory_key_input"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LyraCyan,
                            unfocusedBorderColor = LyraBorderGlow
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Memory Value (e.g. Karima)") },
                        modifier = Modifier.fillMaxWidth().testTag("memory_value_input"),
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newKey.isNotBlank() && newValue.isNotBlank()) {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onSaveMemory(newKey, newValue, MemoryCategory.FACTS)
                                newKey = ""
                                newValue = ""
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LyraCyan),
                        modifier = Modifier.fillMaxWidth().testTag("save_memory_confirm_btn")
                    ) {
                        Text("Save to Vault", color = LyraSurfaceDark, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Memories List
            if (memories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No memories stored yet.\nTalk with LYRA or use Add Memory above.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memories, key = { it.id }) { memory ->
                        MemoryItemCard(memory = memory, onDelete = { onDeleteMemory(memory.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryItemCard(memory: MemoryEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LyraSurfaceCard)
            .border(1.dp, LyraBorderGlow, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = memory.key.replace("_", " ").uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LyraCyan
                )
                if (memory.isVerified) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified Memory",
                        tint = LyraSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = memory.value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Category: ${memory.category.name} • Confidence: ${(memory.confidence * 100).toInt()}%",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_memory_${memory.id}")) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Memory", tint = LyraError.copy(alpha = 0.8f))
        }
    }
}
