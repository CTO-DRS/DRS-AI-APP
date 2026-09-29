package com.drs.ai.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.drs.ai.data.db.PromptTemplate
import kotlinx.coroutines.flow.Flow

/** v1.4 — prompt template library (local only). */
@Dao
interface TemplateDao {
    @Query("SELECT * FROM prompt_templates ORDER BY useCount DESC, createdAt DESC")
    fun observeAll(): Flow<List<PromptTemplate>>

    @Insert
    suspend fun insert(t: PromptTemplate): Long

    @Update
    suspend fun update(t: PromptTemplate)

    @Delete
    suspend fun delete(t: PromptTemplate)

    @Query("UPDATE prompt_templates SET useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementUse(id: Long)

    @Query("SELECT COUNT(*) FROM prompt_templates")
    suspend fun count(): Int

    /** One-time seed so the library is never empty on first open. Returns inserted ids. */
    @Insert
    suspend fun insertAll(t: List<PromptTemplate>): List<Long>
}
