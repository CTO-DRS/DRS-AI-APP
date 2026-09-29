package com.drs.ai.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatSession::class, ChatMessage::class, LocalModel::class, MemoryEntry::class, Document::class, VectorRow::class, Reminder::class],
    version = 2,
    exportSchema = false
)
abstract class DrsDatabase : RoomDatabase() {
    abstract fun chatDao(): com.drs.ai.data.dao.ChatDao
    abstract fun memoryAndModelsDao(): com.drs.ai.data.dao.MemoryAndModelsDao
    abstract fun ragDao(): com.drs.ai.data.dao.RagDao
    abstract fun reminderDao(): com.drs.ai.data.dao.ReminderDao

    companion object {
        @Volatile private var instance: DrsDatabase? = null

        /** v1.3: reminders table — non-destructive migration (chat history preserved). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS reminders (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "note TEXT NOT NULL, " +
                        "triggerAt INTEGER NOT NULL, " +
                        "repeatMode INTEGER NOT NULL, " +
                        "intervalMillis INTEGER NOT NULL, " +
                        "enabled INTEGER NOT NULL, " +
                        "createdAt INTEGER NOT NULL, " +
                        "aiSuggested INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): DrsDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, DrsDatabase::class.java, "drs_ai.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
