package com.gestcar.datos.basedatos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gestcar.datos.dao.GastoPeriodicoDao
import com.gestcar.datos.dao.MantenimientoDao
import com.gestcar.datos.dao.RecordatorioDao
import com.gestcar.datos.dao.RepostajeDao
import com.gestcar.datos.dao.VehiculoDao
import com.gestcar.datos.entidades.GastoPeriodico
import com.gestcar.datos.entidades.Mantenimiento
import com.gestcar.datos.entidades.Recordatorio
import com.gestcar.datos.entidades.Repostaje
import com.gestcar.datos.entidades.Vehiculo

// base de datos principal de la app, aqui se registran todas las entidades
// y se crean los dao para acceder a ellas
@Database(
    entities = [
        Vehiculo::class,
        Repostaje::class,
        Mantenimiento::class,
        GastoPeriodico::class,
        Recordatorio::class
    ],
    version = 4,
    exportSchema = false
)
abstract class GestCarBaseDatos : RoomDatabase() {

    abstract fun vehiculoDao(): VehiculoDao
    abstract fun repostajeDao(): RepostajeDao
    abstract fun mantenimientoDao(): MantenimientoDao
    abstract fun gastoPeriodicoDao(): GastoPeriodicoDao
    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vehiculos_nueva (
                        id TEXT NOT NULL PRIMARY KEY,
                        usuarioId TEXT NOT NULL,
                        marca TEXT NOT NULL,
                        modelo TEXT NOT NULL,
                        anioFabricacion INTEGER NOT NULL,
                        mesFabricacion INTEGER,
                        diaFabricacion INTEGER,
                        tipo TEXT NOT NULL,
                        matricula TEXT NOT NULL,
                        kilometraje REAL NOT NULL,
                        tipoCombustible TEXT,
                        imagenUri TEXT,
                        fechaAlta INTEGER NOT NULL,
                        notas TEXT,
                        actualizadoEn INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO vehiculos_nueva (
                        id,
                        usuarioId,
                        marca,
                        modelo,
                        anioFabricacion,
                        mesFabricacion,
                        diaFabricacion,
                        tipo,
                        matricula,
                        kilometraje,
                        tipoCombustible,
                        imagenUri,
                        fechaAlta,
                        notas,
                        actualizadoEn
                    )
                    SELECT
                        id,
                        usuarioId,
                        marca,
                        modelo,
                        anio,
                        NULL,
                        NULL,
                        tipo,
                        matricula,
                        kilometraje,
                        tipoCombustible,
                        imagenUri,
                        fechaAlta,
                        notas,
                        actualizadoEn
                    FROM vehiculos
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE vehiculos")
                db.execSQL("ALTER TABLE vehiculos_nueva RENAME TO vehiculos")
            }
        }

        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS repostajes (
                        id TEXT NOT NULL PRIMARY KEY,
                        vehiculoId TEXT NOT NULL,
                        fecha INTEGER NOT NULL,
                        kilometros REAL NOT NULL,
                        litros REAL NOT NULL,
                        precioPorLitro REAL NOT NULL,
                        importeTotal REAL NOT NULL,
                        llenoCompleto INTEGER NOT NULL,
                        gasolinera TEXT,
                        notas TEXT,
                        actualizadoEn INTEGER NOT NULL,
                        FOREIGN KEY(vehiculoId) REFERENCES vehiculos(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_repostajes_vehiculoId ON repostajes(vehiculoId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mantenimientos (
                        id TEXT NOT NULL PRIMARY KEY,
                        vehiculoId TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        categoria TEXT NOT NULL,
                        fecha INTEGER NOT NULL,
                        kilometros REAL,
                        coste REAL NOT NULL,
                        taller TEXT,
                        descripcion TEXT,
                        actualizadoEn INTEGER NOT NULL,
                        FOREIGN KEY(vehiculoId) REFERENCES vehiculos(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mantenimientos_vehiculoId ON mantenimientos(vehiculoId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gastos_periodicos (
                        id TEXT NOT NULL PRIMARY KEY,
                        vehiculoId TEXT NOT NULL,
                        concepto TEXT NOT NULL,
                        importe REAL NOT NULL,
                        fecha INTEGER NOT NULL,
                        periodicidad TEXT,
                        fechaVencimiento INTEGER,
                        notas TEXT,
                        actualizadoEn INTEGER NOT NULL,
                        FOREIGN KEY(vehiculoId) REFERENCES vehiculos(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_gastos_periodicos_vehiculoId ON gastos_periodicos(vehiculoId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recordatorios (
                        id TEXT NOT NULL PRIMARY KEY,
                        vehiculoId TEXT NOT NULL,
                        concepto TEXT NOT NULL,
                        fechaLimite INTEGER,
                        kilometrajeLimite REAL,
                        completado INTEGER NOT NULL,
                        fechaCompletado INTEGER,
                        notas TEXT,
                        actualizadoEn INTEGER NOT NULL,
                        FOREIGN KEY(vehiculoId) REFERENCES vehiculos(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recordatorios_vehiculoId ON recordatorios(vehiculoId)")
            }
        }

        private val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE gastos_periodicos
                    ADD COLUMN pagado INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE gastos_periodicos
                    ADD COLUMN fechaPago INTEGER
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCIA: GestCarBaseDatos? = null

        fun obtenerInstancia(contexto: Context): GestCarBaseDatos {
            return INSTANCIA ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    contexto.applicationContext,
                    GestCarBaseDatos::class.java,
                    "gestcar_database"
                ).addMigrations(
                    MIGRACION_1_2,
                    MIGRACION_2_3,
                    MIGRACION_3_4
                ).build()
                INSTANCIA = instancia
                instancia
            }
        }
    }
}
