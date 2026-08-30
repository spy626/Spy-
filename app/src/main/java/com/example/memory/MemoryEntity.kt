package com.example.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemoryCategory {
    USER_PROFILE,
    RELATIONSHIPS,
    PREFERENCES,
    FACTS,
    ROUTINES
}

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: MemoryCategory = MemoryCategory.FACTS,
    val confidence: Float = 1.0f,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isVerified: Boolean = true
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionSummary: String? = null,
    val hasScreenContext: Boolean = false
)
