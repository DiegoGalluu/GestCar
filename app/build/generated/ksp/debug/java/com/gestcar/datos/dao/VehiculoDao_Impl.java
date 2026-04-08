package com.gestcar.datos.dao;

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
import com.gestcar.datos.entidades.Vehiculo;
import java.lang.Class;
import java.lang.Exception;
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
public final class VehiculoDao_Impl implements VehiculoDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Vehiculo> __insertionAdapterOfVehiculo;

  private final EntityDeletionOrUpdateAdapter<Vehiculo> __deletionAdapterOfVehiculo;

  private final EntityDeletionOrUpdateAdapter<Vehiculo> __updateAdapterOfVehiculo;

  private final SharedSQLiteStatement __preparedStmtOfEliminarTodosPorUsuario;

  public VehiculoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfVehiculo = new EntityInsertionAdapter<Vehiculo>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `vehiculos` (`id`,`usuarioId`,`marca`,`modelo`,`anio`,`tipo`,`matricula`,`kilometraje`,`tipoCombustible`,`imagenUri`,`fechaAlta`,`notas`,`actualizadoEn`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Vehiculo entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUsuarioId());
        statement.bindString(3, entity.getMarca());
        statement.bindString(4, entity.getModelo());
        statement.bindLong(5, entity.getAnio());
        statement.bindString(6, entity.getTipo());
        statement.bindString(7, entity.getMatricula());
        statement.bindDouble(8, entity.getKilometraje());
        if (entity.getTipoCombustible() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getTipoCombustible());
        }
        if (entity.getImagenUri() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getImagenUri());
        }
        statement.bindLong(11, entity.getFechaAlta());
        if (entity.getNotas() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getNotas());
        }
        statement.bindLong(13, entity.getActualizadoEn());
      }
    };
    this.__deletionAdapterOfVehiculo = new EntityDeletionOrUpdateAdapter<Vehiculo>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `vehiculos` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Vehiculo entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfVehiculo = new EntityDeletionOrUpdateAdapter<Vehiculo>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `vehiculos` SET `id` = ?,`usuarioId` = ?,`marca` = ?,`modelo` = ?,`anio` = ?,`tipo` = ?,`matricula` = ?,`kilometraje` = ?,`tipoCombustible` = ?,`imagenUri` = ?,`fechaAlta` = ?,`notas` = ?,`actualizadoEn` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Vehiculo entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUsuarioId());
        statement.bindString(3, entity.getMarca());
        statement.bindString(4, entity.getModelo());
        statement.bindLong(5, entity.getAnio());
        statement.bindString(6, entity.getTipo());
        statement.bindString(7, entity.getMatricula());
        statement.bindDouble(8, entity.getKilometraje());
        if (entity.getTipoCombustible() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getTipoCombustible());
        }
        if (entity.getImagenUri() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getImagenUri());
        }
        statement.bindLong(11, entity.getFechaAlta());
        if (entity.getNotas() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getNotas());
        }
        statement.bindLong(13, entity.getActualizadoEn());
        statement.bindString(14, entity.getId());
      }
    };
    this.__preparedStmtOfEliminarTodosPorUsuario = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM vehiculos WHERE usuarioId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertar(final Vehiculo vehiculo, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfVehiculo.insert(vehiculo);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object eliminar(final Vehiculo vehiculo, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfVehiculo.handle(vehiculo);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object actualizar(final Vehiculo vehiculo, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfVehiculo.handle(vehiculo);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object eliminarTodosPorUsuario(final String usuarioId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfEliminarTodosPorUsuario.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, usuarioId);
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
          __preparedStmtOfEliminarTodosPorUsuario.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Vehiculo>> obtenerVehiculosPorUsuario(final String usuarioId) {
    final String _sql = "SELECT * FROM vehiculos WHERE usuarioId = ? ORDER BY fechaAlta DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, usuarioId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"vehiculos"}, new Callable<List<Vehiculo>>() {
      @Override
      @NonNull
      public List<Vehiculo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUsuarioId = CursorUtil.getColumnIndexOrThrow(_cursor, "usuarioId");
          final int _cursorIndexOfMarca = CursorUtil.getColumnIndexOrThrow(_cursor, "marca");
          final int _cursorIndexOfModelo = CursorUtil.getColumnIndexOrThrow(_cursor, "modelo");
          final int _cursorIndexOfAnio = CursorUtil.getColumnIndexOrThrow(_cursor, "anio");
          final int _cursorIndexOfTipo = CursorUtil.getColumnIndexOrThrow(_cursor, "tipo");
          final int _cursorIndexOfMatricula = CursorUtil.getColumnIndexOrThrow(_cursor, "matricula");
          final int _cursorIndexOfKilometraje = CursorUtil.getColumnIndexOrThrow(_cursor, "kilometraje");
          final int _cursorIndexOfTipoCombustible = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoCombustible");
          final int _cursorIndexOfImagenUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imagenUri");
          final int _cursorIndexOfFechaAlta = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaAlta");
          final int _cursorIndexOfNotas = CursorUtil.getColumnIndexOrThrow(_cursor, "notas");
          final int _cursorIndexOfActualizadoEn = CursorUtil.getColumnIndexOrThrow(_cursor, "actualizadoEn");
          final List<Vehiculo> _result = new ArrayList<Vehiculo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Vehiculo _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUsuarioId;
            _tmpUsuarioId = _cursor.getString(_cursorIndexOfUsuarioId);
            final String _tmpMarca;
            _tmpMarca = _cursor.getString(_cursorIndexOfMarca);
            final String _tmpModelo;
            _tmpModelo = _cursor.getString(_cursorIndexOfModelo);
            final int _tmpAnio;
            _tmpAnio = _cursor.getInt(_cursorIndexOfAnio);
            final String _tmpTipo;
            _tmpTipo = _cursor.getString(_cursorIndexOfTipo);
            final String _tmpMatricula;
            _tmpMatricula = _cursor.getString(_cursorIndexOfMatricula);
            final double _tmpKilometraje;
            _tmpKilometraje = _cursor.getDouble(_cursorIndexOfKilometraje);
            final String _tmpTipoCombustible;
            if (_cursor.isNull(_cursorIndexOfTipoCombustible)) {
              _tmpTipoCombustible = null;
            } else {
              _tmpTipoCombustible = _cursor.getString(_cursorIndexOfTipoCombustible);
            }
            final String _tmpImagenUri;
            if (_cursor.isNull(_cursorIndexOfImagenUri)) {
              _tmpImagenUri = null;
            } else {
              _tmpImagenUri = _cursor.getString(_cursorIndexOfImagenUri);
            }
            final long _tmpFechaAlta;
            _tmpFechaAlta = _cursor.getLong(_cursorIndexOfFechaAlta);
            final String _tmpNotas;
            if (_cursor.isNull(_cursorIndexOfNotas)) {
              _tmpNotas = null;
            } else {
              _tmpNotas = _cursor.getString(_cursorIndexOfNotas);
            }
            final long _tmpActualizadoEn;
            _tmpActualizadoEn = _cursor.getLong(_cursorIndexOfActualizadoEn);
            _item = new Vehiculo(_tmpId,_tmpUsuarioId,_tmpMarca,_tmpModelo,_tmpAnio,_tmpTipo,_tmpMatricula,_tmpKilometraje,_tmpTipoCombustible,_tmpImagenUri,_tmpFechaAlta,_tmpNotas,_tmpActualizadoEn);
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
  public Object obtenerPorId(final String id, final Continuation<? super Vehiculo> $completion) {
    final String _sql = "SELECT * FROM vehiculos WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Vehiculo>() {
      @Override
      @Nullable
      public Vehiculo call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUsuarioId = CursorUtil.getColumnIndexOrThrow(_cursor, "usuarioId");
          final int _cursorIndexOfMarca = CursorUtil.getColumnIndexOrThrow(_cursor, "marca");
          final int _cursorIndexOfModelo = CursorUtil.getColumnIndexOrThrow(_cursor, "modelo");
          final int _cursorIndexOfAnio = CursorUtil.getColumnIndexOrThrow(_cursor, "anio");
          final int _cursorIndexOfTipo = CursorUtil.getColumnIndexOrThrow(_cursor, "tipo");
          final int _cursorIndexOfMatricula = CursorUtil.getColumnIndexOrThrow(_cursor, "matricula");
          final int _cursorIndexOfKilometraje = CursorUtil.getColumnIndexOrThrow(_cursor, "kilometraje");
          final int _cursorIndexOfTipoCombustible = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoCombustible");
          final int _cursorIndexOfImagenUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imagenUri");
          final int _cursorIndexOfFechaAlta = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaAlta");
          final int _cursorIndexOfNotas = CursorUtil.getColumnIndexOrThrow(_cursor, "notas");
          final int _cursorIndexOfActualizadoEn = CursorUtil.getColumnIndexOrThrow(_cursor, "actualizadoEn");
          final Vehiculo _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUsuarioId;
            _tmpUsuarioId = _cursor.getString(_cursorIndexOfUsuarioId);
            final String _tmpMarca;
            _tmpMarca = _cursor.getString(_cursorIndexOfMarca);
            final String _tmpModelo;
            _tmpModelo = _cursor.getString(_cursorIndexOfModelo);
            final int _tmpAnio;
            _tmpAnio = _cursor.getInt(_cursorIndexOfAnio);
            final String _tmpTipo;
            _tmpTipo = _cursor.getString(_cursorIndexOfTipo);
            final String _tmpMatricula;
            _tmpMatricula = _cursor.getString(_cursorIndexOfMatricula);
            final double _tmpKilometraje;
            _tmpKilometraje = _cursor.getDouble(_cursorIndexOfKilometraje);
            final String _tmpTipoCombustible;
            if (_cursor.isNull(_cursorIndexOfTipoCombustible)) {
              _tmpTipoCombustible = null;
            } else {
              _tmpTipoCombustible = _cursor.getString(_cursorIndexOfTipoCombustible);
            }
            final String _tmpImagenUri;
            if (_cursor.isNull(_cursorIndexOfImagenUri)) {
              _tmpImagenUri = null;
            } else {
              _tmpImagenUri = _cursor.getString(_cursorIndexOfImagenUri);
            }
            final long _tmpFechaAlta;
            _tmpFechaAlta = _cursor.getLong(_cursorIndexOfFechaAlta);
            final String _tmpNotas;
            if (_cursor.isNull(_cursorIndexOfNotas)) {
              _tmpNotas = null;
            } else {
              _tmpNotas = _cursor.getString(_cursorIndexOfNotas);
            }
            final long _tmpActualizadoEn;
            _tmpActualizadoEn = _cursor.getLong(_cursorIndexOfActualizadoEn);
            _result = new Vehiculo(_tmpId,_tmpUsuarioId,_tmpMarca,_tmpModelo,_tmpAnio,_tmpTipo,_tmpMatricula,_tmpKilometraje,_tmpTipoCombustible,_tmpImagenUri,_tmpFechaAlta,_tmpNotas,_tmpActualizadoEn);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
