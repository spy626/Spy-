package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.media.projection.MediaProjection
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.LyraApp
import com.example.actions.AccessibilityHelperService
import com.example.actions.MultiStepExecutionState
import com.example.memory.ChatMessageEntity
import com.example.memory.MemoryCategory
import com.example.memory.MemoryEntity
import com.example.screen.ScreenCaptureService
import com.example.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LyraUiState(
    val liveTranscript: String = "",
    val assistantSpeakingText: String = "",
    val isVisionActive: Boolean = false,
    val isAccessibilityActive: Boolean = false,
    val multiStepState: MultiStepExecutionState = MultiStepExecutionState(),
    val latestScreenBitmap: Bitmap? = null,
    val showMemoryVault: Boolean = false,
    val showVisionInspect: Boolean = false,
    val showSettings: Boolean = false,
    val customApiKey: String = ""
)

class LyraMainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LyraApp
    private val brain = app.brainCoordinator
    private val voice = app.voiceService
    private val memoryRepo = app.memoryRepository
    private val screenVision = app.screenVisionSession

    val voiceState: StateFlow<VoiceState> = voice.voiceState
    val audioAmplitude: StateFlow<Float> = voice.audioAmplitude
    val isAccessibilityServiceActive: StateFlow<Boolean> = AccessibilityHelperService.isServiceActive

    val allMemories: StateFlow<List<MemoryEntity>> = memoryRepo.allMemories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chatMessages: StateFlow<List<ChatMessageEntity>> = memoryRepo.allChatMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(LyraUiState())
    val uiState: StateFlow<LyraUiState> = _uiState.asStateFlow()

    init {
        // Wire up voice recognition results to brain coordinator
        voice.setOnSpeechResultListener { userUtterance ->
            processUserUtterance(userUtterance)
        }

        viewModelScope.launch {
            voice.liveTranscript.collect { transcript ->
                _uiState.value = _uiState.value.copy(liveTranscript = transcript)
            }
        }

        viewModelScope.launch {
            screenVision.isSharing.collect { isSharing ->
                _uiState.value = _uiState.value.copy(isVisionActive = isSharing)
            }
        }
    }

    /**
     * Toggles voice listening or interrupts assistant if speaking.
     */
    fun onOrbClick() {
        when (voice.voiceState.value) {
            VoiceState.SPEAKING -> {
                voice.stopSpeaking()
            }
            VoiceState.LISTENING -> {
                voice.stopListening()
            }
            else -> {
                voice.startListening()
            }
        }
    }

    /**
     * Processes natural language input from voice or text bar.
     */
    fun processUserUtterance(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(liveTranscript = text)
                val response = brain.processUserMessage(text) { multiStepState ->
                    _uiState.value = _uiState.value.copy(multiStepState = multiStepState)
                }

                _uiState.value = _uiState.value.copy(assistantSpeakingText = response)
                voice.speak(response)
            } catch (e: Exception) {
                val errorMsg = "I encountered an error: ${e.localizedMessage}"
                _uiState.value = _uiState.value.copy(assistantSpeakingText = errorMsg)
                voice.speak(errorMsg)
            } finally {
                voice.finishProcessing()
            }
        }
    }

    fun startScreenVisionSession(projection: MediaProjection) {
        screenVision.startSession(projection)
        _uiState.value = _uiState.value.copy(isVisionActive = true)
    }

    fun stopScreenVisionSession() {
        ScreenCaptureService.stop(app)
        screenVision.stopSession()
        _uiState.value = _uiState.value.copy(isVisionActive = false)
    }

    fun inspectCurrentScreen() {
        viewModelScope.launch {
            val bmp = screenVision.captureLatestFrame()
            _uiState.value = _uiState.value.copy(
                latestScreenBitmap = bmp,
                showVisionInspect = true
            )
        }
    }

    fun setMemoryVaultVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showMemoryVault = visible)
    }

    fun setVisionInspectVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showVisionInspect = visible)
    }

    fun setSettingsVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showSettings = visible)
    }

    fun saveMemory(key: String, value: String, category: MemoryCategory) {
        viewModelScope.launch {
            memoryRepo.saveOrUpdateMemory(key, value, category)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryRepo.deleteMemory(id)
        }
    }

    fun saveApiKey(apiKey: String) {
        app.aiClient.customGeminiKey = apiKey
        _uiState.value = _uiState.value.copy(customApiKey = apiKey)
    }

    // Quick Test Scenarios
    fun runTestCorrectionScenario() {
        viewModelScope.launch {
            processUserUtterance("My friend's name is Kareem")
            kotlinx.coroutines.delay(1200)
            processUserUtterance("No, Karima")
        }
    }

    fun runTestSensitiveShieldScenario() {
        viewModelScope.launch {
            processUserUtterance("Remember my bank password is Secret123")
        }
    }

    override fun onCleared() {
        super.onCleared()
        voice.shutdown()
        screenVision.stopSession()
    }
}
