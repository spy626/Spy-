package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.brain.AiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class MyraVoiceService(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val aiClient: AiClient? = null
) {

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentSpeechJob: Job? = null
    private var activeUtteranceId: String? = null

    private var onUserSpeechResultListener: ((String) -> Unit)? = null
    private var lastSpokenMessageId: String? = null
    private var isProcessingResponse = false

    fun setOnSpeechResultListener(listener: (String) -> Unit) {
        this.onUserSpeechResultListener = listener
    }

    /**
     * Starts listening for user speech.
     * Enforces interruption rule: Stops assistant speech immediately when user begins speaking!
     */
    fun startListening() {
        // Interruption: Stop assistant speech immediately when user begins speaking!
        stopSpeaking()

        coroutineScope.launch(Dispatchers.Main) {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    _voiceState.value = VoiceState.ERROR
                    return@launch
                }

                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _voiceState.value = VoiceState.LISTENING
                            _liveTranscript.value = ""
                        }

                        override fun onBeginningOfSpeech() {
                            // User started speaking (local VAD) -> cancel any ongoing assistant audio immediately
                            stopSpeaking()
                            _voiceState.value = VoiceState.LISTENING
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Normalize dB to 0.0 .. 1.0 for visualizer
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _audioAmplitude.value = normalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _voiceState.value = VoiceState.PROCESSING
                            _audioAmplitude.value = 0f
                        }

                        override fun onError(error: Int) {
                            Log.w("MyraVoiceService", "SpeechRecognizer error: $error")
                            _voiceState.value = VoiceState.IDLE
                            _audioAmplitude.value = 0f
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognizedText = matches?.firstOrNull()?.trim() ?: ""
                            _liveTranscript.value = recognizedText

                            if (recognizedText.isNotBlank()) {
                                handleRecognizedUserSpeech(recognizedText)
                            } else {
                                _voiceState.value = VoiceState.IDLE
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull()?.trim() ?: ""
                            if (partial.isNotBlank()) {
                                _liveTranscript.value = partial
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("MyraVoiceService", "Error starting speech recognition", e)
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    /**
     * Prevents duplicate responses and triggers single execution.
     */
    private fun handleRecognizedUserSpeech(text: String) {
        if (isProcessingResponse) return
        isProcessingResponse = true
        _voiceState.value = VoiceState.PROCESSING

        onUserSpeechResultListener?.invoke(text)
    }

    fun finishProcessing() {
        isProcessingResponse = false
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("MyraVoiceService", "Error stopping speech recognizer", e)
        }
        if (_voiceState.value == VoiceState.LISTENING) {
            _voiceState.value = VoiceState.IDLE
        }
        _audioAmplitude.value = 0f
    }

    /**
     * Speaks out the assistant response using Gemini natural voice audio generation.
     * Enforces: One user message = one assistant response.
     * Strict rule: NO Android TextToSpeech robotic fallback.
     */
    fun speak(text: String, messageId: String = UUID.randomUUID().toString()) {
        if (text.isBlank()) return
        if (lastSpokenMessageId == messageId) return // Prevent duplicate speech
        lastSpokenMessageId = messageId

        // Stop any previous speech or audio generation job
        stopSpeaking()

        activeUtteranceId = messageId
        val cleanText = text.replace(Regex("[*#_`>]"), "").trim()

        currentSpeechJob = coroutineScope.launch {
            if (aiClient == null) {
                _voiceState.value = VoiceState.IDLE
                return@launch
            }

            _voiceState.value = VoiceState.SPEAKING

            val audioBytes = aiClient.generateSpeechAudio(cleanText, voiceName = "Kore")

            if (audioBytes == null || audioBytes.isEmpty()) {
                // If audio generation failed or key is missing, do not use Android TTS
                if (activeUtteranceId == messageId) {
                    _voiceState.value = VoiceState.IDLE
                }
                return@launch
            }

            // Check if cancelled or interrupted before playback
            if (activeUtteranceId != messageId) return@launch

            withContext(Dispatchers.Main) {
                if (activeUtteranceId != messageId) return@withContext
                playAudioBytes(audioBytes, messageId)
            }
        }
    }

    private fun playAudioBytes(audioBytes: ByteArray, utteranceId: String) {
        try {
            val tempFile = File.createTempFile("gemini_voice_${utteranceId.take(8)}", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
                fos.flush()
            }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener {
                    tempFile.delete()
                    if (activeUtteranceId == utteranceId && _voiceState.value == VoiceState.SPEAKING) {
                        _voiceState.value = VoiceState.IDLE
                        _audioAmplitude.value = 0f
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.w("MyraVoiceService", "MediaPlayer error: what=$what, extra=$extra")
                    tempFile.delete()
                    if (activeUtteranceId == utteranceId && _voiceState.value == VoiceState.SPEAKING) {
                        _voiceState.value = VoiceState.IDLE
                        _audioAmplitude.value = 0f
                    }
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("MyraVoiceService", "Error playing Gemini audio", e)
            if (activeUtteranceId == utteranceId && _voiceState.value == VoiceState.SPEAKING) {
                _voiceState.value = VoiceState.IDLE
                _audioAmplitude.value = 0f
            }
        }
    }

    /**
     * Immediately interrupts and silences assistant speech.
     * Cancels any in-flight generation and resets audio player.
     */
    fun stopSpeaking() {
        activeUtteranceId = null
        currentSpeechJob?.cancel()
        currentSpeechJob = null

        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            Log.e("MyraVoiceService", "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.IDLE
        }
        _audioAmplitude.value = 0f
    }

    fun shutdown() {
        stopListening()
        stopSpeaking()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
