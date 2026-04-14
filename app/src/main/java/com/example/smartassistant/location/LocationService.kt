package com.example.smartassistant.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class LocationService(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // 修復：移至 IO Thread 避免阻塞 UI
    suspend fun getCoordsFromAddress(address: String): LatLng? = withContext(Dispatchers.IO) {
        return@withContext try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                LatLng(addresses[0].latitude, addresses[0].longitude)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // 修復：取得真實裝置位置
    @SuppressLint("MissingPermission") // 權限已在 MainActivity 檢查
    suspend fun getCurrentLocation(): LatLng? = withContext(Dispatchers.IO) {
        return@withContext try {
            val location: Location? = fusedLocationClient.lastLocation.await()
            if (location != null) {
                LatLng(location.latitude, location.longitude)
            } else {
                LatLng(25.0478, 121.5170) // 找不到定位時的 fallback (台北車站)
            }
        } catch (e: Exception) {
            LatLng(25.0478, 121.5170)
        }
    }
}