package com.drs.ai.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val model: String? = null,
    // v1.4 — session management + smart context compression
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
    val summary: String? = null,          // rolling local summary of older turns
    @ColumnInfo(defaultValue = "0") val summaryUpTo: Long = 0 // messages with createdAt <= this are covered by summary
)
