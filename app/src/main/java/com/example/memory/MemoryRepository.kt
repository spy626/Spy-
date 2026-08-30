package com.example.memory

import kotlinx.coroutines.flow.Flow
import java.util.Locale

class MemoryRepository(private val memoryDao: MemoryDao) {

    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()
    val allChatMessages: Flow<List<ChatMessageEntity>> = memoryDao.getAllChatMessages()

    private val sensitivePatterns = listOf(
        "password", "passcode", "pin code", "otp", "one time password",
        "cvv", "credit card", "debit card", "ssn", "social security",
        "bank account", "routing number", "secret key", "auth token", "seed phrase"
    )

    fun isSensitive(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return sensitivePatterns.any { pattern -> lower.contains(pattern) }
    }

    suspend fun saveOrUpdateMemory(
        key: String,
        value: String,
        category: MemoryCategory = MemoryCategory.FACTS
    ): MemoryResult {
        // Enforce safety rule: Never store passwords, OTP, banking secrets
        if (isSensitive(key) || isSensitive(value)) {
            return MemoryResult.RejectedSensitive(
                "LYRA Privacy Shield: Rejected saving sensitive credentials or security secrets."
            )
        }

        val normalizedKey = key.trim().lowercase(Locale.ROOT).replace(" ", "_")
        val existing = memoryDao.getMemoryByKey(normalizedKey)

        return if (existing != null) {
            val updated = existing.copy(
                value = value.trim(),
                lastUpdated = System.currentTimeMillis(),
                isVerified = true
            )
            memoryDao.updateMemory(updated)
            MemoryResult.Updated(previousValue = existing.value, newValue = value.trim(), memory = updated)
        } else {
            val newMemory = MemoryEntity(
                key = normalizedKey,
                value = value.trim(),
                category = category,
                lastUpdated = System.currentTimeMillis(),
                isVerified = true
            )
            val id = memoryDao.insertMemory(newMemory)
            MemoryResult.Saved(memory = newMemory.copy(id = id))
        }
    }

    suspend fun findMemory(keyQuery: String): MemoryEntity? {
        val normalized = keyQuery.trim().lowercase(Locale.ROOT).replace(" ", "_")
        val direct = memoryDao.getMemoryByKey(normalized)
        if (direct != null) return direct

        val searchList = memoryDao.searchMemories(keyQuery.trim())
        return searchList.firstOrNull()
    }

    suspend fun searchMemories(query: String): List<MemoryEntity> {
        return memoryDao.searchMemories(query.trim())
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun deleteMemoryByKey(key: String) {
        val normalized = key.trim().lowercase(Locale.ROOT).replace(" ", "_")
        memoryDao.deleteMemoryByKey(normalized)
    }

    suspend fun clearAll() {
        memoryDao.clearAllMemories()
    }

    suspend fun recordChatMessage(
        role: String,
        content: String,
        actionSummary: String? = null,
        hasScreenContext: Boolean = false
    ) {
        memoryDao.insertChatMessage(
            ChatMessageEntity(
                role = role,
                content = content,
                actionSummary = actionSummary,
                hasScreenContext = hasScreenContext,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun getRecentChat(limit: Int = 10): List<ChatMessageEntity> {
        return memoryDao.getRecentChatMessages(limit).reversed()
    }

    suspend fun clearChat() {
        memoryDao.clearChatHistory()
    }
}

sealed class MemoryResult {
    data class Saved(val memory: MemoryEntity) : MemoryResult()
    data class Updated(val previousValue: String, val newValue: String, val memory: MemoryEntity) : MemoryResult()
    data class RejectedSensitive(val reason: String) : MemoryResult()
}
