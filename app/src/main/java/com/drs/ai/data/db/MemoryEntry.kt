package com.drs.ai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_entries")
data class MemoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val enabled: Boolean = true,
    val createdAt: Long
)
