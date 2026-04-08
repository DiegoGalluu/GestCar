package com.gestcar.datos.basedatos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gestcar.datos.dao.VehiculoDao
import com.gestcar.datos.entidades.Vehiculo

// base de datos principal de la app, aqui se registran todas las entidades
// y se crean los dao para acceder a ellas
// version 1 porque es la primera version del esquema
@Database(
    entities = [Vehiculo::class],
    version = 1,
    exportSchema = false
)
abstract class GestCarBaseDatos : RoomDatabase() {

    // dao para acceder a la tabla de vehiculos
    abstract fun vehiculoDao(): VehiculoDao

    companion object {
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
                ).build()
                INSTANCIA = instancia
                instancia
            }
        }
    }
}
