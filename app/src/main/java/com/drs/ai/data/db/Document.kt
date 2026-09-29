package com.drs.ai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sizeBytes: Long,
    val chunks: Int,
    val createdAt: Long
)
