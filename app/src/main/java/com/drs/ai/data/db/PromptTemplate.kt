package com.drs.ai.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** v1.4 — saved prompt templates ("مكتبة الأوامر"): fully local, per-device. */
@Entity(tableName = "prompt_templates")
data class PromptTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    @ColumnInfo(defaultValue = "") val category: String = "", // e.g. general | writing | code | ideas
    @ColumnInfo(defaultValue = "0") val useCount: Int = 0,
    val createdAt: Long
)
