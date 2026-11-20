package com.example.ecoswap

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Coordinates(val latitude: Double, val longitude: Double)

object LocationUtils {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? =
        withContext(Dispatchers.IO) {
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(context)
            return@withContext try {
                val location = Tasks.await(fusedLocationClient.lastLocation)
                if (location != null) {
                    Log.d(
                        "LocationUtils",
                        "Usuario en: ${location.latitude}, ${location.longitude}"
                    )
                } else {
                    Log.d("LocationUtils", "No se pudo obtener la ubicación (es null)")
                }
                location
            } catch (e: Exception) {
                Log.e("LocationUtils", "Error obteniendo ubicación", e)
                null
            }
        }
}