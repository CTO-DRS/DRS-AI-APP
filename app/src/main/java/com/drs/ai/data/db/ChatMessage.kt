package com.drs.ai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user" | "assistant"
    val content: String,
    val createdAt: Long,
    val tokens: Int = 0,
    val tokPerSec: Float = 0f,
    val sources: String? = null // RAG sources, comma separated doc names
)
