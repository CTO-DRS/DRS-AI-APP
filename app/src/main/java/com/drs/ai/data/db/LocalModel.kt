package com.drs.ai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_models")
data class LocalModel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val kind: String, // "chat" | "embedder" | "mmproj" | "whisper"
    val arch: String? = null,
    val ctxLen: Long? = null,
    val quant: String? = null,
    val active: Boolean = false,
    val addedAt: Long
)
