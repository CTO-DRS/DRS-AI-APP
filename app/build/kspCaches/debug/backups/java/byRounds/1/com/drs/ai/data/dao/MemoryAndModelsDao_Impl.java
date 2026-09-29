package com.drs.ai.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.drs.ai.data.db.LocalModel;
import com.drs.ai.data.db.MemoryEntry;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MemoryAndModelsDao_Impl implements MemoryAndModelsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MemoryEntry> __insertionAdapterOfMemoryEntry;

  private final EntityInsertionAdapter<LocalModel> __insertionAdapterOfLocalModel;

  private final EntityDeletionOrUpdateAdapter<MemoryEntry> __updateAdapterOfMemoryEntry;

  private final EntityDeletionOrUpdateAdapter<LocalModel> __updateAdapterOfLocalModel;

  private final SharedSQLiteStatement __preparedStmtOfDeleteMemory;

  private final SharedSQLiteStatement __preparedStmtOfClearMemories;

  private final SharedSQLiteStatement __preparedStmtOfDeleteModel;

  private final SharedSQLiteStatement __preparedStmtOfClearActiveForKind;

  private final SharedSQLiteStatement __preparedStmtOfClearModels;

  public MemoryAndModelsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMemoryEntry = new EntityInsertionAdapter<MemoryEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `memory_entries` (`id`,`content`,`enabled`,`createdAt`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MemoryEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getContent());
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindLong(4, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfLocalModel = new EntityInsertionAdapter<LocalModel>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `local_models` (`id`,`name`,`path`,`sizeBytes`,`kind`,`arch`,`ctxLen`,`quant`,`active`,`addedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalModel entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getPath());
        statement.bindLong(4, entity.getSizeBytes());
        statement.bindString(5, entity.getKind());
        if (entity.getArch() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getArch());
        }
        if (entity.getCtxLen() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getCtxLen());
        }
        if (entity.getQuant() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getQuant());
        }
        final int _tmp = entity.getActive() ? 1 : 0;
        statement.bindLong(9, _tmp);
        statement.bindLong(10, entity.getAddedAt());
      }
    };
    this.__updateAdapterOfMemoryEntry = new EntityDeletionOrUpdateAdapter<MemoryEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `memory_entries` SET `id` = ?,`content` = ?,`enabled` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MemoryEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getContent());
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindLong(4, entity.getCreatedAt());
        statement.bindLong(5, entity.getId());
      }
    };
    this.__updateAdapterOfLocalModel = new EntityDeletionOrUpdateAdapter<LocalModel>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `local_models` SET `id` = ?,`name` = ?,`path` = ?,`sizeBytes` = ?,`kind` = ?,`arch` = ?,`ctxLen` = ?,`quant` = ?,`active` = ?,`addedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalModel entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getPath());
        statement.bindLong(4, entity.getSizeBytes());
        statement.bindString(5, entity.getKind());
        if (entity.getArch() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getArch());
        }
        if (entity.getCtxLen() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getCtxLen());
        }
        if (entity.getQuant() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getQuant());
        }
        final int _tmp = entity.getActive() ? 1 : 0;
        statement.bindLong(9, _tmp);
        statement.bindLong(10, entity.getAddedAt());
        statement.bindLong(11, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteMemory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM memory_entries WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearMemories = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM memory_entries";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteModel = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM local_models WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearActiveForKind = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE local_models SET active = 0 WHERE kind = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearModels = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM local_models";
        return _query;
      }
    };
  }

  @Override
  public Object insertMemory(final MemoryEntry e, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMemoryEntry.insertAndReturnId(e);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertModel(final LocalModel m, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfLocalModel.insertAndReturnId(m);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMemory(final MemoryEntry e, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMemoryEntry.handle(e);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateModel(final LocalModel m, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfLocalModel.handle(m);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteMemory(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteMemory.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteMemory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearMemories(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearMemories.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearMemories.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteModel(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteModel.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteModel.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearActiveForKind(final String kind,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearActiveForKind.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, kind);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearActiveForKind.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearModels(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearModels.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearModels.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MemoryEntry>> observeMemories() {
    final String _sql = "SELECT * FROM memory_entries ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"memory_entries"}, new Callable<List<MemoryEntry>>() {
      @Override
      @NonNull
      public List<MemoryEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MemoryEntry> _result = new ArrayList<MemoryEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MemoryEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MemoryEntry(_tmpId,_tmpContent,_tmpEnabled,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object enabledMemories(final Continuation<? super List<MemoryEntry>> $completion) {
    final String _sql = "SELECT * FROM memory_entries WHERE enabled = 1 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MemoryEntry>>() {
      @Override
      @NonNull
      public List<MemoryEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MemoryEntry> _result = new ArrayList<MemoryEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MemoryEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MemoryEntry(_tmpId,_tmpContent,_tmpEnabled,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<LocalModel>> observeModels() {
    final String _sql = "SELECT * FROM local_models ORDER BY addedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"local_models"}, new Callable<List<LocalModel>>() {
      @Override
      @NonNull
      public List<LocalModel> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "sizeBytes");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfArch = CursorUtil.getColumnIndexOrThrow(_cursor, "arch");
          final int _cursorIndexOfCtxLen = CursorUtil.getColumnIndexOrThrow(_cursor, "ctxLen");
          final int _cursorIndexOfQuant = CursorUtil.getColumnIndexOrThrow(_cursor, "quant");
          final int _cursorIndexOfActive = CursorUtil.getColumnIndexOrThrow(_cursor, "active");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<LocalModel> _result = new ArrayList<LocalModel>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalModel _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpSizeBytes;
            _tmpSizeBytes = _cursor.getLong(_cursorIndexOfSizeBytes);
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final String _tmpArch;
            if (_cursor.isNull(_cursorIndexOfArch)) {
              _tmpArch = null;
            } else {
              _tmpArch = _cursor.getString(_cursorIndexOfArch);
            }
            final Long _tmpCtxLen;
            if (_cursor.isNull(_cursorIndexOfCtxLen)) {
              _tmpCtxLen = null;
            } else {
              _tmpCtxLen = _cursor.getLong(_cursorIndexOfCtxLen);
            }
            final String _tmpQuant;
            if (_cursor.isNull(_cursorIndexOfQuant)) {
              _tmpQuant = null;
            } else {
              _tmpQuant = _cursor.getString(_cursorIndexOfQuant);
            }
            final boolean _tmpActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfActive);
            _tmpActive = _tmp != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _item = new LocalModel(_tmpId,_tmpName,_tmpPath,_tmpSizeBytes,_tmpKind,_tmpArch,_tmpCtxLen,_tmpQuant,_tmpActive,_tmpAddedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getModel(final long id, final Continuation<? super LocalModel> $completion) {
    final String _sql = "SELECT * FROM local_models WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalModel>() {
      @Override
      @Nullable
      public LocalModel call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "sizeBytes");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfArch = CursorUtil.getColumnIndexOrThrow(_cursor, "arch");
          final int _cursorIndexOfCtxLen = CursorUtil.getColumnIndexOrThrow(_cursor, "ctxLen");
          final int _cursorIndexOfQuant = CursorUtil.getColumnIndexOrThrow(_cursor, "quant");
          final int _cursorIndexOfActive = CursorUtil.getColumnIndexOrThrow(_cursor, "active");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final LocalModel _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpSizeBytes;
            _tmpSizeBytes = _cursor.getLong(_cursorIndexOfSizeBytes);
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final String _tmpArch;
            if (_cursor.isNull(_cursorIndexOfArch)) {
              _tmpArch = null;
            } else {
              _tmpArch = _cursor.getString(_cursorIndexOfArch);
            }
            final Long _tmpCtxLen;
            if (_cursor.isNull(_cursorIndexOfCtxLen)) {
              _tmpCtxLen = null;
            } else {
              _tmpCtxLen = _cursor.getLong(_cursorIndexOfCtxLen);
            }
            final String _tmpQuant;
            if (_cursor.isNull(_cursorIndexOfQuant)) {
              _tmpQuant = null;
            } else {
              _tmpQuant = _cursor.getString(_cursorIndexOfQuant);
            }
            final boolean _tmpActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfActive);
            _tmpActive = _tmp != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _result = new LocalModel(_tmpId,_tmpName,_tmpPath,_tmpSizeBytes,_tmpKind,_tmpArch,_tmpCtxLen,_tmpQuant,_tmpActive,_tmpAddedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getActiveModel(final String kind,
      final Continuation<? super LocalModel> $completion) {
    final String _sql = "SELECT * FROM local_models WHERE kind = ? AND active = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, kind);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalModel>() {
      @Override
      @Nullable
      public LocalModel call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "sizeBytes");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfArch = CursorUtil.getColumnIndexOrThrow(_cursor, "arch");
          final int _cursorIndexOfCtxLen = CursorUtil.getColumnIndexOrThrow(_cursor, "ctxLen");
          final int _cursorIndexOfQuant = CursorUtil.getColumnIndexOrThrow(_cursor, "quant");
          final int _cursorIndexOfActive = CursorUtil.getColumnIndexOrThrow(_cursor, "active");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final LocalModel _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpSizeBytes;
            _tmpSizeBytes = _cursor.getLong(_cursorIndexOfSizeBytes);
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final String _tmpArch;
            if (_cursor.isNull(_cursorIndexOfArch)) {
              _tmpArch = null;
            } else {
              _tmpArch = _cursor.getString(_cursorIndexOfArch);
            }
            final Long _tmpCtxLen;
            if (_cursor.isNull(_cursorIndexOfCtxLen)) {
              _tmpCtxLen = null;
            } else {
              _tmpCtxLen = _cursor.getLong(_cursorIndexOfCtxLen);
            }
            final String _tmpQuant;
            if (_cursor.isNull(_cursorIndexOfQuant)) {
              _tmpQuant = null;
            } else {
              _tmpQuant = _cursor.getString(_cursorIndexOfQuant);
            }
            final boolean _tmpActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfActive);
            _tmpActive = _tmp != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _result = new LocalModel(_tmpId,_tmpName,_tmpPath,_tmpSizeBytes,_tmpKind,_tmpArch,_tmpCtxLen,_tmpQuant,_tmpActive,_tmpAddedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<LocalModel> observeActiveModel(final String kind) {
    final String _sql = "SELECT * FROM local_models WHERE kind = ? AND active = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, kind);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"local_models"}, new Callable<LocalModel>() {
      @Override
      @Nullable
      public LocalModel call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "sizeBytes");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfArch = CursorUtil.getColumnIndexOrThrow(_cursor, "arch");
          final int _cursorIndexOfCtxLen = CursorUtil.getColumnIndexOrThrow(_cursor, "ctxLen");
          final int _cursorIndexOfQuant = CursorUtil.getColumnIndexOrThrow(_cursor, "quant");
          final int _cursorIndexOfActive = CursorUtil.getColumnIndexOrThrow(_cursor, "active");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final LocalModel _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpSizeBytes;
            _tmpSizeBytes = _cursor.getLong(_cursorIndexOfSizeBytes);
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final String _tmpArch;
            if (_cursor.isNull(_cursorIndexOfArch)) {
              _tmpArch = null;
            } else {
              _tmpArch = _cursor.getString(_cursorIndexOfArch);
            }
            final Long _tmpCtxLen;
            if (_cursor.isNull(_cursorIndexOfCtxLen)) {
              _tmpCtxLen = null;
            } else {
              _tmpCtxLen = _cursor.getLong(_cursorIndexOfCtxLen);
            }
            final String _tmpQuant;
            if (_cursor.isNull(_cursorIndexOfQuant)) {
              _tmpQuant = null;
            } else {
              _tmpQuant = _cursor.getString(_cursorIndexOfQuant);
            }
            final boolean _tmpActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfActive);
            _tmpActive = _tmp != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _result = new LocalModel(_tmpId,_tmpName,_tmpPath,_tmpSizeBytes,_tmpKind,_tmpArch,_tmpCtxLen,_tmpQuant,_tmpActive,_tmpAddedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
