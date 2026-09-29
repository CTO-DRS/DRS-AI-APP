package com.drs.ai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.drs.ai.data.db.LocalModel
import com.drs.ai.data.db.MemoryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryAndModelsDao {
    // Memory
    @Insert
    suspend fun insertMemory(e: MemoryEntry): Long

    @Update
    suspend fun updateMemory(e: MemoryEntry)

    @Query("DELETE FROM memory_entries WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("SELECT * FROM memory_entries ORDER BY createdAt DESC")
    fun observeMemories(): Flow<List<MemoryEntry>>

    @Query("SELECT * FROM memory_entries WHERE enabled = 1 ORDER BY createdAt DESC")
    suspend fun enabledMemories(): List<MemoryEntry>

    @Query("DELETE FROM memory_entries")
    suspend fun clearMemories()

    // Models
    @Insert
    suspend fun insertModel(m: LocalModel): Long

    @Update
    suspend fun updateModel(m: LocalModel)

    @Query("DELETE FROM local_models WHERE id = :id")
    suspend fun deleteModel(id: Long)

    @Query("SELECT * FROM local_models ORDER BY addedAt DESC")
    fun observeModels(): Flow<List<LocalModel>>

    @Query("SELECT * FROM local_models WHERE id = :id")
    suspend fun getModel(id: Long): LocalModel?

    @Query("SELECT * FROM local_models WHERE kind = :kind AND active = 1 LIMIT 1")
    suspend fun getActiveModel(kind: String): LocalModel?

    @Query("SELECT * FROM local_models WHERE kind = :kind AND active = 1 LIMIT 1")
    fun observeActiveModel(kind: String): Flow<LocalModel?>

    @Query("UPDATE local_models SET active = 0 WHERE kind = :kind")
    suspend fun clearActiveForKind(kind: String)

    @Query("DELETE FROM local_models")
    suspend fun clearModels()
}
