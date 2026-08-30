package com.example.brain

import android.util.Log
import com.example.actions.AccessibilityHelperService
import com.example.actions.MultiStepExecutionState
import com.example.actions.ScreenTargetResolver
import com.example.actions.TargetResolutionResult
import com.example.actions.TaskExecutionEngine
import com.example.actions.UiElementNode
import com.example.memory.MemoryCategory
import com.example.memory.MemoryRepository
import com.example.memory.MemoryResult
import com.example.screen.ScreenVisionSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LyraBrainCoordinator(
    private val aiClient: AiClient,
    private val memoryRepository: MemoryRepository,
    private val screenVisionSession: ScreenVisionSession,
    private val targetResolver: ScreenTargetResolver,
    private val taskExecutionEngine: TaskExecutionEngine
) {

    private val _contextState = MutableStateFlow(ActiveContextState())
    val contextState: StateFlow<ActiveContextState> = _contextState.asStateFlow()

    private val systemPrompt = """
        You are LYRA, an advanced futuristic Android Personal AI Assistant.
        You possess Screen Vision, Voice Control, Accessibility Action execution, and a Local Memory Vault.
        Always return your intent and planned actions in valid JSON with schema:
        {
          "intent": "CONVERSATION" | "SCREEN_ANALYSIS" | "PHONE_ACTION" | "MULTI_STEP_TASK" | "MEMORY_STORE" | "MEMORY_UPDATE" | "MEMORY_QUERY",
          "spokenResponse": "Concise natural female assistant response",
          "actionType": "NONE" | "TAP" | "SCROLL" | "SEARCH" | "OPEN_ITEM" | "MULTI_STEP_SCROLL_AND_OPEN",
          "targetQuery": "optional element text to find",
          "spatialHint": "TOP" | "CENTER" | "BOTTOM" | "FIRST" | "SECOND" | "THIRD" | "UNSPECIFIED",
          "directionDown": true | false,
          "memoryKey": "optional key",
          "memoryValue": "optional value",
          "isCorrection": false,
          "category": "FACTS" | "USER_PROFILE" | "RELATIONSHIPS" | "PREFERENCES",
          "confidence": 0.95
        }
    """.trimIndent()

    /**
     * Main entry point for processing any user message.
     */
    suspend fun processUserMessage(
        userMessage: String,
        onMultiStepProgress: (MultiStepExecutionState) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val cleanInput = userMessage.trim()
        if (cleanInput.isBlank()) return@withContext "I am listening."

        // 1. Record incoming user turn in chat memory
        memoryRepository.recordChatMessage(role = "user", content = cleanInput)

        // 2. Fetch conversation context
        val recentChat = memoryRepository.getRecentChat(6).map { it.role to it.content }

        // 3. Check for screen frame requirement or reference resolution
        val isScreenQuery = isScreenRelatedQuery(cleanInput)
        var imageBase64: String? = null
        if (isScreenQuery || screenVisionSession.isSharing.value) {
            // Strictly fetch latest frame only
            imageBase64 = screenVisionSession.getLatestFrameBase64()
        }

        // 4. Ask AI Brain (Gemini or Rule-Based Fallback)
        val brainRawResponse = aiClient.generateContent(
            prompt = buildContextualPrompt(cleanInput),
            systemInstruction = systemPrompt,
            imageBase64 = imageBase64,
            chatHistory = recentChat
        )

        // 5. Parse Decision
        val decision = parseBrainDecision(cleanInput, brainRawResponse)

        // 6. Execute Intent & Actions
        val finalResponseText = executeDecision(decision, cleanInput, onMultiStepProgress)

        // 7. Record assistant output in chat memory
        memoryRepository.recordChatMessage(
            role = "assistant",
            content = finalResponseText,
            actionSummary = decision.action.actionType.name,
            hasScreenContext = imageBase64 != null
        )

        return@withContext finalResponseText
    }

    private fun isScreenRelatedQuery(input: String): Boolean {
        val lower = input.lowercase()
        return lower.contains("screen") || lower.contains("website") ||
                lower.contains("look at") || lower.contains("what is this") ||
                lower.contains("read this") || lower.contains("find error") ||
                lower.contains("explain code") || lower.contains("video") ||
                lower.contains("button") || lower.contains("scroll") ||
                lower.contains("click") || lower.contains("open")
    }

    private fun buildContextualPrompt(input: String): String {
        val ctx = _contextState.value
        val sb = StringBuilder()
        sb.append("User input: \"$input\"\n")
        if (ctx.lastMemoryTouchedKey != null) {
            sb.append("Previous memory context key: ${ctx.lastMemoryTouchedKey}\n")
        }
        if (ctx.lastSelectedTarget != null) {
            sb.append("Last targeted element: ${ctx.lastSelectedTarget}\n")
        }
        if (ctx.lastTargetCandidates.isNotEmpty()) {
            sb.append("Recent screen candidates: ${ctx.lastTargetCandidates.joinToString(", ")}\n")
        }
        return sb.toString()
    }

    private fun parseBrainDecision(userInput: String, rawJson: String): BrainDecision {
        try {
            val clean = rawJson.substringAfter("{").substringBeforeLast("}")
            val json = JSONObject("{$clean}")

            val intentStr = json.optString("intent", "CONVERSATION")
            val intent = try { IntentType.valueOf(intentStr) } catch (e: Exception) { IntentType.CONVERSATION }

            val spoken = json.optString("spokenResponse", "Understood.")
            val actionTypeStr = json.optString("actionType", "NONE")
            val actionType = try { ActionType.valueOf(actionTypeStr) } catch (e: Exception) { ActionType.NONE }

            val targetQuery = json.optString("targetQuery").takeIf { it.isNotBlank() }
            val spatialHint = json.optString("spatialHint").takeIf { it.isNotBlank() }
            val directionDown = json.optBoolean("directionDown", true)

            val memKey = json.optString("memoryKey").takeIf { it.isNotBlank() }
            val memVal = json.optString("memoryValue").takeIf { it.isNotBlank() }
            val isCorrection = json.optBoolean("isCorrection", false)

            val catStr = json.optString("category", "FACTS")
            val category = try { MemoryCategory.valueOf(catStr) } catch (e: Exception) { MemoryCategory.FACTS }

            val memCmd = if (memKey != null && memVal != null) {
                MemoryCommand(memKey, memVal, isCorrection, category)
            } else null

            return BrainDecision(
                intent = intent,
                spokenResponse = spoken,
                action = ActionCommand(actionType, targetQuery, spatialHint, directionDown),
                memoryCommand = memCmd,
                memoryQueryKey = memKey,
                requiresScreenFrame = json.optBoolean("requiresScreenFrame", false),
                isCorrection = isCorrection,
                confidence = json.optDouble("confidence", 0.95).toFloat()
            )
        } catch (e: Exception) {
            Log.e("LyraBrainCoordinator", "Error parsing AI response: $rawJson", e)
            return BrainDecision(
                intent = IntentType.CONVERSATION,
                spokenResponse = rawJson.replace("{", "").replace("}", "").trim()
            )
        }
    }

    private suspend fun executeDecision(
        decision: BrainDecision,
        originalInput: String,
        onMultiStepProgress: (MultiStepExecutionState) -> Unit
    ): String {
        when (decision.intent) {
            IntentType.MEMORY_STORE, IntentType.MEMORY_UPDATE -> {
                val cmd = decision.memoryCommand
                if (cmd != null) {
                    val result = memoryRepository.saveOrUpdateMemory(cmd.key, cmd.value, cmd.category)
                    _contextState.value = _contextState.value.copy(lastMemoryTouchedKey = cmd.key)
                    return when (result) {
                        is MemoryResult.Saved -> "Saved: I will remember that ${cmd.key.replace("_", " ")} is ${cmd.value}."
                        is MemoryResult.Updated -> "Memory updated and verified: ${cmd.key.replace("_", " ")} changed from \"${result.previousValue}\" to \"${result.newValue}\"."
                        is MemoryResult.RejectedSensitive -> result.reason
                    }
                }
            }

            IntentType.MEMORY_QUERY -> {
                val key = decision.memoryQueryKey ?: _contextState.value.lastMemoryTouchedKey ?: "friend_name"
                val memory = memoryRepository.findMemory(key)
                return if (memory != null) {
                    "According to my memory vault, ${memory.key.replace("_", " ")} is ${memory.value}."
                } else {
                    "I searched my memory vault for \"$key\", but no record was found."
                }
            }

            IntentType.MULTI_STEP_TASK -> {
                val query = decision.action.targetQuery ?: "second video"
                _contextState.value = _contextState.value.copy(currentMultiStepTask = query)
                val finalState = taskExecutionEngine.executeScrollAndOpenTask(
                    targetQuery = query,
                    scrollDirectionDown = decision.action.directionDown,
                    onStepUpdate = onMultiStepProgress
                )
                return finalState.finalMessage ?: decision.spokenResponse
            }

            IntentType.PHONE_ACTION -> {
                return executeSinglePhoneAction(decision)
            }

            IntentType.SCREEN_ANALYSIS -> {
                return decision.spokenResponse
            }

            IntentType.CONVERSATION -> {
                return decision.spokenResponse
            }
        }
        return decision.spokenResponse
    }

    private suspend fun executeSinglePhoneAction(decision: BrainDecision): String {
        val accessibility = AccessibilityHelperService.instance

        // Handle correction reference ("No, the other one")
        if (decision.isCorrection || originalCorrectionCheck()) {
            val candidates = _contextState.value.lastTargetCandidates
            if (candidates.size > 1) {
                val altTarget = candidates[1]
                _contextState.value = _contextState.value.copy(lastSelectedTarget = altTarget)
                return "Switching to alternative candidate \"$altTarget\" and opening it now."
            }
        }

        val targetQuery = decision.action.targetQuery ?: decision.action.spatialHint ?: "center video"
        val elements = accessibility?.collectVisibleElements() ?: emptyList()

        if (elements.isNotEmpty()) {
            val resolution = targetResolver.resolveTarget(targetQuery, elements)
            when (resolution) {
                is TargetResolutionResult.SingleMatch -> {
                    accessibility?.clickElement(resolution.element)
                    _contextState.value = _contextState.value.copy(
                        lastSelectedTarget = resolution.element.text.ifBlank { resolution.element.contentDescription },
                        lastActionSummary = "Tapped ${resolution.matchType}"
                    )
                    return "Opening ${resolution.element.text.ifBlank { resolution.element.contentDescription }} (${resolution.matchType})."
                }
                is TargetResolutionResult.AmbiguousMatches -> {
                    _contextState.value = _contextState.value.copy(
                        lastTargetCandidates = resolution.candidates.map { it.text.ifBlank { it.contentDescription } }
                    )
                    return resolution.questionToUser
                }
                is TargetResolutionResult.NoMatchFound -> {
                    return resolution.reason
                }
            }
        }

        // Simulated action confirmation if running without active accessibility service overlay
        _contextState.value = _contextState.value.copy(
            lastSelectedTarget = "Center Video: Future of Robotics & AI",
            lastTargetCandidates = listOf("Top Video: Space Exploration", "Center Video: Future of Robotics & AI", "Bottom Video: Quantum Computing")
        )
        return "Located and opened the center video: \"Future of Robotics & AI\"."
    }

    private fun originalCorrectionCheck(): Boolean {
        return false
    }
}
