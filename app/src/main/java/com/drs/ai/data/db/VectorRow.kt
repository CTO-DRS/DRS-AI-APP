package com.drs.ai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vector_rows")
data class VectorRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val docId: Long,
    val chunkIndex: Int,
    val text: String,
    val dim: Int,
    val vec: ByteArray
)
