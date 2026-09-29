package com.drs.ai.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChatSession::class, ChatMessage::class, LocalModel::class, MemoryEntry::class, Document::class, VectorRow::class],
    version = 1,
    exportSchema = false
)
abstract class DrsDatabase : RoomDatabase() {
    abstract fun chatDao(): com.drs.ai.data.dao.ChatDao
    abstract fun memoryAndModelsDao(): com.drs.ai.data.dao.MemoryAndModelsDao
    abstract fun ragDao(): com.drs.ai.data.dao.RagDao

    companion object {
        @Volatile private var instance: DrsDatabase? = null

        fun get(context: Context): DrsDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, DrsDatabase::class.java, "drs_ai.db")
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
