package com.gestcar.datos.basedatos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gestcar.datos.dao.VehiculoDao
import com.gestcar.datos.entidades.Vehiculo

// base de datos principal de la app, aqui se registran todas las entidades
// y se crean los dao para acceder a ellas
// version 1 porque es la primera version del esquema
@Database(
    entities = [Vehiculo::class],
    version = 2,
    exportSchema = false
)
abstract class GestCarBaseDatos : RoomDatabase() {

    // dao para acceder a la tabla de vehiculos
    abstract fun vehiculoDao(): VehiculoDao

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

        // instancia unica de la base de datos, se usa el patron singleton
        // para que no se creen multiples conexiones a la vez
        @Volatile
        private var INSTANCIA: GestCarBaseDatos? = null

        fun obtenerInstancia(contexto: Context): GestCarBaseDatos {
            // si ya hay una instancia creada la devolvemos directamente
            return INSTANCIA ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    contexto.applicationContext,
                    GestCarBaseDatos::class.java,
                    "gestcar_database"
                ).addMigrations(MIGRACION_1_2).build()
                INSTANCIA = instancia
                instancia
            }
        }
    }
}
