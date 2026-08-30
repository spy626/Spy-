package com.example.brain

import com.example.memory.MemoryCategory

enum class IntentType {
    CONVERSATION,
    SCREEN_ANALYSIS,
    PHONE_ACTION,
    MULTI_STEP_TASK,
    MEMORY_STORE,
    MEMORY_UPDATE,
    MEMORY_QUERY
}

data class ActionCommand(
    val actionType: ActionType,
    val targetQuery: String? = null,
    val spatialHint: String? = null,
    val directionDown: Boolean = true
)

enum class ActionType {
    NONE,
    TAP,
    SCROLL,
    SEARCH,
    OPEN_ITEM,
    MULTI_STEP_SCROLL_AND_OPEN
}

data class MemoryCommand(
    val key: String,
    val value: String,
    val isCorrection: Boolean = false,
    val category: MemoryCategory = MemoryCategory.FACTS
)

data class BrainDecision(
    val intent: IntentType,
    val spokenResponse: String,
    val action: ActionCommand = ActionCommand(ActionType.NONE),
    val memoryCommand: MemoryCommand? = null,
    val memoryQueryKey: String? = null,
    val requiresScreenFrame: Boolean = false,
    val isCorrection: Boolean = false,
    val confidence: Float = 0.95f
)

data class ActiveContextState(
    val lastTargetCandidates: List<String> = emptyList(),
    val lastSelectedTarget: String? = null,
    val lastActionSummary: String? = null,
    val lastMemoryTouchedKey: String? = null,
    val currentMultiStepTask: String? = null,
    val screenAnalysisSummary: String? = null
)
