package com.drs.ai.core.memory

import com.drs.ai.data.dao.MemoryAndModelsDao
import com.drs.ai.data.db.MemoryEntry
import kotlinx.coroutines.flow.Flow

/**
 * User-controlled local memory. Nothing is remembered implicitly — entries are
 * explicit, individually toggleable, and injectable into prompts only when enabled.
 */
class MemoryManager(private val dao: MemoryAndModelsDao) {

    fun observe(): Flow<List<MemoryEntry>> = dao.observeMemories()

    suspend fun add(content: String): Long {
        val text = content.trim()
        if (text.isEmpty()) return -1
        return dao.insertMemory(MemoryEntry(content = text, enabled = true, createdAt = System.currentTimeMillis()))
    }

    suspend fun toggle(entry: MemoryEntry) = dao.updateMemory(entry.copy(enabled = !entry.enabled))

    suspend fun delete(entry: MemoryEntry) = dao.deleteMemory(entry.id)

    suspend fun clearAll() = dao.clearMemories()

    /** Build the prompt injection block; capped to keep prompt budget sane. */
    suspend fun buildBlock(maxChars: Int = 2000): String? {
        val list = dao.enabledMemories()
        if (list.isEmpty()) return null
        val sb = StringBuilder("User-stored memories (long-term facts the user asked you to remember):\n")
        var used = 0
        for (m in list) {
            val line = "- ${m.content}\n"
            if (used + line.length > maxChars) break
            sb.append(line)
            used += line.length
        }
        return sb.toString().trim()
    }
}
