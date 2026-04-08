package com.gestcar.datos.basedatos;

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
import com.gestcar.datos.dao.VehiculoDao;
import com.gestcar.datos.dao.VehiculoDao_Impl;
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
public final class GestCarBaseDatos_Impl extends GestCarBaseDatos {
  private volatile VehiculoDao _vehiculoDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `vehiculos` (`id` TEXT NOT NULL, `usuarioId` TEXT NOT NULL, `marca` TEXT NOT NULL, `modelo` TEXT NOT NULL, `anio` INTEGER NOT NULL, `tipo` TEXT NOT NULL, `matricula` TEXT NOT NULL, `kilometraje` REAL NOT NULL, `tipoCombustible` TEXT, `imagenUri` TEXT, `fechaAlta` INTEGER NOT NULL, `notas` TEXT, `actualizadoEn` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3b06a598a8afdc58f584b56b0067dce5')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `vehiculos`");
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
        final HashMap<String, TableInfo.Column> _columnsVehiculos = new HashMap<String, TableInfo.Column>(13);
        _columnsVehiculos.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVehiculos.put("usuarioId", new TableInfo.Column("usuarioId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVehiculos.put("marca", new TableInfo.Column("marca", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVehiculos.put("modelo", new TableInfo.Column("modelo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVehiculos.put("anio", new TableInfo.Column("anio", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVehiculos.put("tipo", new TableInfo.Column("tipo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVehiculos.put("matricula", new TableInfo.Column("matricula", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVehiculos.put("kilometraje", new TableInfo.Column("kilometraje", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVehiculos.put("tipoCombustible", new TableInfo.Column("tipoCombustible", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVehiculos.put("imagenUri", new TableInfo.Column("imagenUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVehiculos.put("fechaAlta", new TableInfo.Column("fechaAlta", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVehiculos.put("notas", new TableInfo.Column("notas", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVehiculos.put("actualizadoEn", new TableInfo.Column("actualizadoEn", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVehiculos = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVehiculos = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoVehiculos = new TableInfo("vehiculos", _columnsVehiculos, _foreignKeysVehiculos, _indicesVehiculos);
        final TableInfo _existingVehiculos = TableInfo.read(db, "vehiculos");
        if (!_infoVehiculos.equals(_existingVehiculos)) {
          return new RoomOpenHelper.ValidationResult(false, "vehiculos(com.gestcar.datos.entidades.Vehiculo).\n"
                  + " Expected:\n" + _infoVehiculos + "\n"
                  + " Found:\n" + _existingVehiculos);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "3b06a598a8afdc58f584b56b0067dce5", "6c57cb6a6529eb44e9e2f89bdeb88161");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "vehiculos");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `vehiculos`");
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
    _typeConvertersMap.put(VehiculoDao.class, VehiculoDao_Impl.getRequiredConverters());
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
  public VehiculoDao vehiculoDao() {
    if (_vehiculoDao != null) {
      return _vehiculoDao;
    } else {
      synchronized(this) {
        if(_vehiculoDao == null) {
          _vehiculoDao = new VehiculoDao_Impl(this);
        }
        return _vehiculoDao;
      }
    }
  }
}
