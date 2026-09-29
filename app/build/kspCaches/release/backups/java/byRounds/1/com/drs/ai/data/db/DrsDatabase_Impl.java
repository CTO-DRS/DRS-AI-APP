package com.drs.ai.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.drs.ai.data.dao.ChatDao;
import com.drs.ai.data.dao.ChatDao_Impl;
import com.drs.ai.data.dao.MemoryAndModelsDao;
import com.drs.ai.data.dao.MemoryAndModelsDao_Impl;
import com.drs.ai.data.dao.RagDao;
import com.drs.ai.data.dao.RagDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DrsDatabase_Impl extends DrsDatabase {
  private volatile ChatDao _chatDao;

  private volatile MemoryAndModelsDao _memoryAndModelsDao;

  private volatile RagDao _ragDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `chat_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `model` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `tokens` INTEGER NOT NULL, `tokPerSec` REAL NOT NULL, `sources` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `local_models` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `path` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `kind` TEXT NOT NULL, `arch` TEXT, `ctxLen` INTEGER, `quant` TEXT, `active` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `memory_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `documents` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `chunks` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `vector_rows` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `docId` INTEGER NOT NULL, `chunkIndex` INTEGER NOT NULL, `text` TEXT NOT NULL, `dim` INTEGER NOT NULL, `vec` BLOB NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ad049a816289a6cc54d1fdbca45ff32c')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `chat_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `chat_messages`");
        db.execSQL("DROP TABLE IF EXISTS `local_models`");
        db.execSQL("DROP TABLE IF EXISTS `memory_entries`");
        db.execSQL("DROP TABLE IF EXISTS `documents`");
        db.execSQL("DROP TABLE IF EXISTS `vector_rows`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsChatSessions = new HashMap<String, TableInfo.Column>(5);
        _columnsChatSessions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatSessions.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatSessions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatSessions.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatSessions.put("model", new TableInfo.Column("model", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChatSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChatSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChatSessions = new TableInfo("chat_sessions", _columnsChatSessions, _foreignKeysChatSessions, _indicesChatSessions);
        final TableInfo _existingChatSessions = TableInfo.read(db, "chat_sessions");
        if (!_infoChatSessions.equals(_existingChatSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "chat_sessions(com.drs.ai.data.db.ChatSession).\n"
                  + " Expected:\n" + _infoChatSessions + "\n"
                  + " Found:\n" + _existingChatSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsChatMessages = new HashMap<String, TableInfo.Column>(8);
        _columnsChatMessages.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("sessionId", new TableInfo.Column("sessionId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("role", new TableInfo.Column("role", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("tokens", new TableInfo.Column("tokens", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("tokPerSec", new TableInfo.Column("tokPerSec", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("sources", new TableInfo.Column("sources", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChatMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChatMessages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChatMessages = new TableInfo("chat_messages", _columnsChatMessages, _foreignKeysChatMessages, _indicesChatMessages);
        final TableInfo _existingChatMessages = TableInfo.read(db, "chat_messages");
        if (!_infoChatMessages.equals(_existingChatMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "chat_messages(com.drs.ai.data.db.ChatMessage).\n"
                  + " Expected:\n" + _infoChatMessages + "\n"
                  + " Found:\n" + _existingChatMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsLocalModels = new HashMap<String, TableInfo.Column>(10);
        _columnsLocalModels.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalModels.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalModels.put("path", new TableInfo.Column("path", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalModels.put("sizeBytes", new TableInfo.Column("sizeBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalModels.put("kind", new TableInfo.Column("kind", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalModels.put("arch", new TableInfo.Column("arch", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalModels.put("ctxLen", new TableInfo.Column("ctxLen", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalModels.put("quant", new TableInfo.Column("quant", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalModels.put("active", new TableInfo.Column("active", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalModels.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLocalModels = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLocalModels = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLocalModels = new TableInfo("local_models", _columnsLocalModels, _foreignKeysLocalModels, _indicesLocalModels);
        final TableInfo _existingLocalModels = TableInfo.read(db, "local_models");
        if (!_infoLocalModels.equals(_existingLocalModels)) {
          return new RoomOpenHelper.ValidationResult(false, "local_models(com.drs.ai.data.db.LocalModel).\n"
                  + " Expected:\n" + _infoLocalModels + "\n"
                  + " Found:\n" + _existingLocalModels);
        }
        final HashMap<String, TableInfo.Column> _columnsMemoryEntries = new HashMap<String, TableInfo.Column>(4);
        _columnsMemoryEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMemoryEntries.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMemoryEntries.put("enabled", new TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMemoryEntries.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMemoryEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMemoryEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMemoryEntries = new TableInfo("memory_entries", _columnsMemoryEntries, _foreignKeysMemoryEntries, _indicesMemoryEntries);
        final TableInfo _existingMemoryEntries = TableInfo.read(db, "memory_entries");
        if (!_infoMemoryEntries.equals(_existingMemoryEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "memory_entries(com.drs.ai.data.db.MemoryEntry).\n"
                  + " Expected:\n" + _infoMemoryEntries + "\n"
                  + " Found:\n" + _existingMemoryEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsDocuments = new HashMap<String, TableInfo.Column>(5);
        _columnsDocuments.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocuments.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocuments.put("sizeBytes", new TableInfo.Column("sizeBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocuments.put("chunks", new TableInfo.Column("chunks", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocuments.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDocuments = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDocuments = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDocuments = new TableInfo("documents", _columnsDocuments, _foreignKeysDocuments, _indicesDocuments);
        final TableInfo _existingDocuments = TableInfo.read(db, "documents");
        if (!_infoDocuments.equals(_existingDocuments)) {
          return new RoomOpenHelper.ValidationResult(false, "documents(com.drs.ai.data.db.Document).\n"
                  + " Expected:\n" + _infoDocuments + "\n"
                  + " Found:\n" + _existingDocuments);
        }
        final HashMap<String, TableInfo.Column> _columnsVectorRows = new HashMap<String, TableInfo.Column>(6);
        _columnsVectorRows.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVectorRows.put("docId", new TableInfo.Column("docId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVectorRows.put("chunkIndex", new TableInfo.Column("chunkIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVectorRows.put("text", new TableInfo.Column("text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVectorRows.put("dim", new TableInfo.Column("dim", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVectorRows.put("vec", new TableInfo.Column("vec", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVectorRows = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVectorRows = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoVectorRows = new TableInfo("vector_rows", _columnsVectorRows, _foreignKeysVectorRows, _indicesVectorRows);
        final TableInfo _existingVectorRows = TableInfo.read(db, "vector_rows");
        if (!_infoVectorRows.equals(_existingVectorRows)) {
          return new RoomOpenHelper.ValidationResult(false, "vector_rows(com.drs.ai.data.db.VectorRow).\n"
                  + " Expected:\n" + _infoVectorRows + "\n"
                  + " Found:\n" + _existingVectorRows);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "ad049a816289a6cc54d1fdbca45ff32c", "7e52ae5cc6c4f0457e7fa873ad7a995b");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "chat_sessions","chat_messages","local_models","memory_entries","documents","vector_rows");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `chat_sessions`");
      _db.execSQL("DELETE FROM `chat_messages`");
      _db.execSQL("DELETE FROM `local_models`");
      _db.execSQL("DELETE FROM `memory_entries`");
      _db.execSQL("DELETE FROM `documents`");
      _db.execSQL("DELETE FROM `vector_rows`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ChatDao.class, ChatDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MemoryAndModelsDao.class, MemoryAndModelsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RagDao.class, RagDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ChatDao chatDao() {
    if (_chatDao != null) {
      return _chatDao;
    } else {
      synchronized(this) {
        if(_chatDao == null) {
          _chatDao = new ChatDao_Impl(this);
        }
        return _chatDao;
      }
    }
  }

  @Override
  public MemoryAndModelsDao memoryAndModelsDao() {
    if (_memoryAndModelsDao != null) {
      return _memoryAndModelsDao;
    } else {
      synchronized(this) {
        if(_memoryAndModelsDao == null) {
          _memoryAndModelsDao = new MemoryAndModelsDao_Impl(this);
        }
        return _memoryAndModelsDao;
      }
    }
  }

  @Override
  public RagDao ragDao() {
    if (_ragDao != null) {
      return _ragDao;
    } else {
      synchronized(this) {
        if(_ragDao == null) {
          _ragDao = new RagDao_Impl(this);
        }
        return _ragDao;
      }
    }
  }
}
