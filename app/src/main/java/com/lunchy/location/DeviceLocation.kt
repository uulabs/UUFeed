package com.lunchy.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.lunchy.domain.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class LabeledPoint(
    val point: GeoPoint,
    val label: String,
)

class DeviceLocation(
    private val context: Context,
) {
    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun current(): GeoPoint? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = providerNames()
        val last = providers.mapNotNull { name ->
            runCatching { manager.getLastKnownLocation(name) }.getOrNull()
        }.maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time <= STALE_MS) {
            return last.toPoint()
        }
        val enabled = providers.filter { name ->
            runCatching { manager.isProviderEnabled(name) }.getOrDefault(false)
        }
        val fresh = requestFresh(manager, enabled)
        return fresh ?: last?.toPoint()
    }

    suspend fun geocode(query: String): LabeledPoint? = withTimeoutOrNull(8_000) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withTimeoutOrNull null
        val geocoder = Geocoder(context, Locale.forLanguageTag("en-AU"))
        val addresses = fetchName(geocoder, trimmed)
        val address = addresses.firstOrNull { it.countryCode.equals("AU", ignoreCase = true) }
            ?: addresses.firstOrNull()
            ?: return@withTimeoutOrNull null
        LabeledPoint(GeoPoint(address.latitude, address.longitude), address.friendlyLabel())
    }

    suspend fun reverse(point: GeoPoint): String? = withTimeoutOrNull(8_000) {
        val geocoder = Geocoder(context, Locale.forLanguageTag("en-AU"))
        val address = fetchPoint(geocoder, point).firstOrNull() ?: return@withTimeoutOrNull null
        address.friendlyLabel()
    }

    private suspend fun requestFresh(manager: LocationManager, providers: List<String>): GeoPoint? {
        if (providers.isEmpty()) return null
        return withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { manager.removeUpdates(this) }
                        if (continuation.isActive) continuation.resume(location.toPoint())
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) = Unit
                }
                continuation.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
                var requested = false
                for (provider in providers) {
                    val ok = runCatching {
                        manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                    }.isSuccess
                    requested = requested || ok
                }
                if (!requested && continuation.isActive) continuation.resume(null)
            }
        }
    }

    private suspend fun fetchName(geocoder: Geocoder, query: String): List<Address> {
        if (!Geocoder.isPresent()) return emptyList()
        return if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(
                    query,
                    5,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(emptyList())
                        }
                    },
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocationName(query, 5) }.getOrNull().orEmpty()
            }
        }
    }

    private suspend fun fetchPoint(geocoder: Geocoder, point: GeoPoint): List<Address> {
        if (!Geocoder.isPresent()) return emptyList()
        return if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    point.latitude,
                    point.longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(emptyList())
                        }
                    },
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(point.latitude, point.longitude, 1) }.getOrNull().orEmpty()
            }
        }
    }

    private fun providerNames(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
        add(LocationManager.GPS_PROVIDER)
    }

    private fun Location.toPoint() = GeoPoint(latitude, longitude)

    private fun Address.friendlyLabel(): String {
        val bits = listOfNotNull(subLocality, locality, adminArea).distinct()
        if (bits.isNotEmpty()) return bits.joinToString(", ")
        return featureName ?: getAddressLine(0) ?: "Selected place"
    }

    companion object {
        private const val STALE_MS = 2 * 60 * 1000L
    }
}
