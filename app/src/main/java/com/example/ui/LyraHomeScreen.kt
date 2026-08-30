package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memory.ChatMessageEntity
import com.example.screen.ScreenCaptureService
import com.example.ui.components.ActionHudBanner
import com.example.ui.components.HolographicOrb
import com.example.ui.components.MemoryVaultSheet
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TaskExecutionCard
import com.example.ui.components.VisionInspectDialog
import com.example.ui.theme.LyraBorderGlow
import com.example.ui.theme.LyraCyan
import com.example.ui.theme.LyraObsidian
import com.example.ui.theme.LyraPink
import com.example.ui.theme.LyraSky
import com.example.ui.theme.LyraSuccess
import com.example.ui.theme.LyraSurfaceCard
import com.example.ui.theme.LyraSurfaceDark
import com.example.ui.theme.LyraViolet
import com.example.voice.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyraHomeScreen(
    viewModel: LyraMainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val audioAmplitude by viewModel.audioAmplitude.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityServiceActive.collectAsState()
    val memories by viewModel.allMemories.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Screen capture permission launcher
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.start(context, result.resultCode, result.data!!)
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(LyraCyan, LyraViolet)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("L", fontWeight = FontWeight.Black, fontSize = 16.sp, color = LyraObsidian)
                        }
                        Column {
                            Text(
                                text = "LYRA",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "PERSONAL AI ASSISTANT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LyraCyan,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.inspectCurrentScreen() },
                        modifier = Modifier.testTag("top_action_vision")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Inspect Vision",
                            tint = if (uiState.isVisionActive) LyraCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setMemoryVaultVisible(true) },
                        modifier = Modifier.testTag("top_action_memory")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Memory Vault",
                            tint = LyraViolet
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setSettingsVisible(true) },
                        modifier = Modifier.testTag("top_action_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LyraObsidian
                )
            )
        },
        containerColor = LyraObsidian,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status & Quick HUD
            ActionHudBanner(
                isVisionActive = uiState.isVisionActive,
                isAccessibilityActive = isAccessibilityActive,
                memoryCount = memories.size,
                onVisionClick = {
                    if (!uiState.isVisionActive) {
                        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
                    } else {
                        viewModel.inspectCurrentScreen()
                    }
                },
                onAccessibilityClick = {
                    try {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // ignore
                    }
                },
                onMemoryClick = { viewModel.setMemoryVaultVisible(true) },
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            // Multi-step task execution feedback card
            AnimatedVisibility(
                visible = uiState.multiStepState.steps.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TaskExecutionCard(
                    state = uiState.multiStepState,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Central Holographic Orb & State Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HolographicOrb(
                        voiceState = voiceState,
                        audioAmplitude = audioAmplitude,
                        onClick = { viewModel.onOrbClick() }
                    )

                    // State Tag
                    val (statusText, statusColor) = when (voiceState) {
                        VoiceState.LISTENING -> "LISTENING... (TAP TO STOP)" to LyraCyan
                        VoiceState.PROCESSING -> "THINKING & ANALYZING..." to LyraViolet
                        VoiceState.SPEAKING -> "SPEAKING (TAP TO INTERRUPT)" to LyraPink
                        VoiceState.ERROR -> "SYSTEM READY" to LyraCyan
                        VoiceState.IDLE -> "TAP ORB TO SPEAK" to LyraSky
                    }

                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Quick Prompt Suggestions Carousel
            QuickSuggestionsRow(
                onSelectQuery = { query ->
                    viewModel.processUserUtterance(query)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Conversation Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    item {
                        EmptyChatGreetingCard()
                    }
                } else {
                    items(chatMessages, key = { it.id }) { message ->
                        ChatMessageBubble(message = message)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            text = if (uiState.liveTranscript.isNotBlank()) uiState.liveTranscript else "Ask LYRA anything...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("user_chat_input"),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank()) {
                                val msg = textInput
                                textInput = ""
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                viewModel.processUserUtterance(msg)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LyraSurfaceCard,
                        unfocusedContainerColor = LyraSurfaceCard,
                        focusedBorderColor = LyraCyan,
                        unfocusedBorderColor = LyraBorderGlow,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 3
                )

                // Send Button
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val msg = textInput
                            textInput = ""
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.processUserUtterance(msg)
                        } else {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.onOrbClick()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (textInput.isNotBlank()) listOf(LyraCyan, LyraViolet) else listOf(LyraViolet, LyraPink)
                            )
                        )
                        .testTag("send_or_mic_button")
                ) {
                    Icon(
                        imageVector = if (textInput.isNotBlank()) Icons.AutoMirrored.Filled.Send else if (voiceState == VoiceState.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Action",
                        tint = LyraObsidian
                    )
                }
            }
        }
    }

    // Dialogs & Sheets
    if (uiState.showMemoryVault) {
        MemoryVaultSheet(
            memories = memories,
            onDismiss = { viewModel.setMemoryVaultVisible(false) },
            onSaveMemory = { key, value, cat -> viewModel.saveMemory(key, value, cat) },
            onDeleteMemory = { id -> viewModel.deleteMemory(id) },
            onQuickTestCorrection = {
                viewModel.setMemoryVaultVisible(false)
                viewModel.runTestCorrectionScenario()
            },
            onQuickTestSensitiveShield = {
                viewModel.setMemoryVaultVisible(false)
                viewModel.runTestSensitiveShieldScenario()
            }
        )
    }

    if (uiState.showVisionInspect) {
        VisionInspectDialog(
            latestBitmap = uiState.latestScreenBitmap,
            isSharing = uiState.isVisionActive,
            onDismiss = { viewModel.setVisionInspectVisible(false) },
            onExecuteSampleQuery = { query ->
                viewModel.setVisionInspectVisible(false)
                viewModel.processUserUtterance(query)
            }
        )
    }

    if (uiState.showSettings) {
        SettingsDialog(
            currentApiKey = uiState.customApiKey,
            onSaveApiKey = { key -> viewModel.saveApiKey(key) },
            onDismiss = { viewModel.setSettingsVisible(false) }
        )
    }
}

@Composable
private fun QuickSuggestionsRow(onSelectQuery: (String) -> Unit) {
    val suggestions = listOf(
        "Scroll down and open the second video",
        "Center video open karo",
        "What is on my screen?",
        "My friend's name is Kareem",
        "What is my friend's name?",
        "Explain visible code"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(suggestions) { item ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(LyraSurfaceCard.copy(alpha = 0.8f))
                    .border(1.dp, LyraBorderGlow, RoundedCornerShape(16.dp))
                    .clickable { onSelectQuery(item) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = item,
                    fontSize = 11.sp,
                    color = LyraCyan,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmptyChatGreetingCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LyraSurfaceCard.copy(alpha = 0.5f))
            .border(1.dp, LyraBorderGlow, RoundedCornerShape(18.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = LyraCyan,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "Welcome, Commander",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "LYRA is your personal AI companion equipped with Screen Vision, Real-time Voice control, Multi-Step automation, and an Encrypted Memory Vault.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
                    else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                )
                .border(
                    1.dp,
                    if (isUser) LyraCyan.copy(alpha = 0.4f) else LyraViolet.copy(alpha = 0.4f),
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "YOU" else "LYRA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) LyraCyan else LyraPink,
                        letterSpacing = 1.sp
                    )
                    if (message.hasScreenContext) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = LyraCyan, modifier = Modifier.size(11.dp))
                            Text("Screen Vision", fontSize = 9.sp, color = LyraCyan)
                        }
                    }
                }

                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )

                if (!message.actionSummary.isNullOrBlank() && message.actionSummary != "NONE") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LyraViolet.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Action: ${message.actionSummary}",
                            fontSize = 10.sp,
                            color = LyraViolet,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
