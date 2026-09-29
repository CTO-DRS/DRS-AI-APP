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

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSession(id: Long): ChatSession?

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
