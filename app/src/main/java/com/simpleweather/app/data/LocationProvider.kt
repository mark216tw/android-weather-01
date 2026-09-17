package com.simpleweather.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.simpleweather.app.model.Place
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed interface LocationResult {
    data class Success(val place: Place, val isStale: Boolean) : LocationResult
    data object PermissionRequired : LocationResult
    data object ProviderDisabled : LocationResult
    data object Unavailable : LocationResult
}

class LocationProvider(
    private val context: Context,
    private val client: FusedLocationProviderClient,
) {
    @SuppressLint("MissingPermission")
    suspend fun resolveCurrentPlace(): LocationResult {
        if (!hasLocationPermission()) return LocationResult.PermissionRequired
        val locationManager = context.getSystemService(LocationManager::class.java)
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) return LocationResult.ProviderDisabled

        val last = runCatching { client.lastLocation.await() }.getOrNull()
        if (last != null && last.ageMillis() <= FRESH_AGE && last.accuracy <= MAX_ACCURACY_METERS) {
            return LocationResult.Success(last.toPlace(), false)
        }

        val cancellation = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setDurationMillis(CURRENT_TIMEOUT)
            .build()
        val current = withTimeoutOrNull(CURRENT_TIMEOUT) {
            runCatching { client.getCurrentLocation(request, cancellation.token).await() }.getOrNull()
        }
        cancellation.cancel()
        if (current != null) return LocationResult.Success(current.toPlace(), false)
        if (last != null && last.ageMillis() <= STALE_FALLBACK_AGE) {
            return LocationResult.Success(last.toPlace(), true)
        }
        return LocationResult.Unavailable
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private suspend fun Location.toPlace(): Place {
        val address = runCatching { reverseGeocode(latitude, longitude) }.getOrNull()
        val name = listOf(address?.locality, address?.subAdminArea, address?.adminArea, address?.countryName)
            .firstOrNull { !it.isNullOrBlank() } ?: "目前位置"
        return Place(
            id = "current_location",
            name = name,
            adminArea = address?.adminArea,
            country = address?.countryName,
            countryCode = address?.countryCode,
            latitude = latitude,
            longitude = longitude,
            isCurrentLocation = true,
        )
    }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): Address? =
        withTimeoutOrNull(GEOCODE_TIMEOUT) {
            val geocoder = Geocoder(context, Locale.TRADITIONAL_CHINESE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                withContext(Dispatchers.IO) {
                    runCatching { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }.getOrNull()
                }
            }
        }

    private fun Location.ageMillis(): Long = (System.currentTimeMillis() - time).coerceAtLeast(0)

    private companion object {
        const val FRESH_AGE = 30 * 60 * 1000L
        const val STALE_FALLBACK_AGE = 24 * 60 * 60 * 1000L
        const val CURRENT_TIMEOUT = 10_000L
        const val GEOCODE_TIMEOUT = 5_000L
        const val MAX_ACCURACY_METERS = 10_000f
    }
}
