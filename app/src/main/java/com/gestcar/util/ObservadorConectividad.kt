package com.gestcar.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class ObservadorConectividad(
    contexto: Context
) {
    private val gestorConectividad = contexto.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observarConexion(): Flow<Boolean> = callbackFlow {
        trySend(hayConexion())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(hayConexion())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(hayConexion())
            }
        }

        gestorConectividad.registerDefaultNetworkCallback(callback)

        awaitClose {
            gestorConectividad.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    private fun hayConexion(): Boolean {
        val redActiva = gestorConectividad.activeNetwork ?: return false
        val capacidades = gestorConectividad.getNetworkCapabilities(redActiva) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
