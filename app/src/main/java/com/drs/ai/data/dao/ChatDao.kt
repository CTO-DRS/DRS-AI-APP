package com.drs.ai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.drs.ai.data.db.ChatMessage
import com.drs.ai.data.db.ChatSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insertSession(s: ChatSession): Long

    @Update
    suspend fun updateSession(s: ChatSession)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM chat_sessions ORDER BY pinned DESC, updatedAt DESC")
    fun observeSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSession(id: Long): ChatSession?

    // v1.4 — session search / pin / rename
    @Query("SELECT * FROM chat_sessions WHERE title LIKE '%' || :q || '%' ORDER BY pinned DESC, updatedAt DESC")
    fun searchSessions(q: String): Flow<List<ChatSession>>

    @Query("UPDATE chat_sessions SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE chat_sessions SET title = :title WHERE id = :id")
    suspend fun renameSession(id: Long, title: String)

    // v1.4 — usage statistics (honest, local-only)
    @Query("SELECT COUNT(*) FROM chat_sessions")
    suspend fun sessionCount(): Int

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun messageCountTotal(): Int

    @Query("SELECT COALESCE(SUM(tokens), 0) FROM chat_messages WHERE role = 'assistant'")
    suspend fun totalTokens(): Int

    @Query("SELECT COALESCE(AVG(tokPerSec), 0) FROM chat_messages WHERE role = 'assistant' AND tokPerSec > 0")
    suspend fun avgTokPerSec(): Float

    @Insert
    suspend fun insertMessage(m: ChatMessage): Long

    @Update
    suspend fun updateMessage(m: ChatMessage)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeMessages(sessionId: Long): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun messagesForSession(sessionId: Long): List<ChatMessage>

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND createdAt >= :fromTime")
    suspend fun deleteMessagesFrom(sessionId: Long, fromTime: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearSessionMessages(sessionId: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM chat_sessions")
    suspend fun clearAllSessions()

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun messageCount(sessionId: Long): Int
}
