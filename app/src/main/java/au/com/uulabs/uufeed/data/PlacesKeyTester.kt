package au.com.uulabs.uufeed.data

import au.com.uulabs.uufeed.data.dto.GoogleErrorEnvelope
import au.com.uulabs.uufeed.data.legacy.LegacyEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

sealed interface KeyTestResult {
    data class Success(val message: String, val backend: PlacesBackend) : KeyTestResult
    data class Failed(val message: String) : KeyTestResult
}

/**
 * Tries Places API (New) first. A key that only has the legacy Places API
 * comes back PERMISSION_DENIED or "API not enabled", and then a small Nearby
 * Search is tried. The working backend is what the caller should save.
 */
class PlacesKeyTester(
    private val http: OkHttpClient,
    private val packageName: String,
    private val certSha1: String,
    private val json: Json,
    private val newSearchUrl: String = NEW_SEARCH_URL,
    private val legacyNearbyUrl: String = LEGACY_NEARBY_URL,
) {
    suspend fun test(apiKey: String): KeyTestResult = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            return@withContext KeyTestResult.Failed("Paste a key first.")
        }
        val modern = try {
            probeNew(trimmed)
        } catch (_: IOException) {
            return@withContext KeyTestResult.Failed("Couldn't reach Google. Check your connection and try again.")
        }
        if (modern is Probe.Ok) {
            return@withContext KeyTestResult.Success(
                "Using Google Places (New). Google accepted the key. This check only asks for place IDs, which stays on the free Essentials tier.",
                PlacesBackend.NEW,
            )
        }
        val denied = modern as Probe.Denied
        if (!denied.tryLegacy) {
            return@withContext KeyTestResult.Failed(denied.message)
        }
        val legacy = try {
            probeLegacy(trimmed)
        } catch (_: IOException) {
            return@withContext KeyTestResult.Failed(
                "Places API (New) is not available for this key, and the legacy Places API could not be reached. Check your connection and try again.",
            )
        }
        when (legacy) {
            is Probe.Ok -> KeyTestResult.Success(
                "Using Google Places (legacy).",
                PlacesBackend.LEGACY,
            )
            is Probe.Denied -> KeyTestResult.Failed(
                "This key could not use Places API (New) or the legacy Places API. " +
                    "Enable one of them on the Google Cloud project. Some new billing accounts have to prepay before an API turns on. " +
                    "Places API (New): ${denied.message} Legacy Places API: ${legacy.message}",
            )
        }
    }

    private fun probeNew(apiKey: String): Probe {
        val body = """{"textQuery":"cafe","pageSize":1,"regionCode":"AU"}"""
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(newSearchUrl)
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", "places.id")
            .apply { addAndroidHeaders(this, packageName, certSha1) }
            .post(body)
            .build()
        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (response.isSuccessful) return Probe.Ok
            val parsed = runCatching { json.decodeFromString(GoogleErrorEnvelope.serializer(), raw) }.getOrNull()
            val status = parsed?.error?.status
            val message = parsed?.error?.message ?: "Google rejected the key (${response.code})."
            val tryLegacy = newApiNeedsLegacyFallback(response.code, status, message)
            val hint = when {
                tryLegacy -> ""
                response.code == 429 || status == "RESOURCE_EXHAUSTED" -> " The quota for this key is used up."
                response.code == 400 -> " The key was rejected before a Places product could be chosen."
                else -> " Check that billing is on and any Android restriction uses package au.com.uulabs.uufeed plus the SHA-1 on the setup screen."
            }
            return Probe.Denied(message + hint, tryLegacy)
        }
    }

    private fun probeLegacy(apiKey: String): Probe {
        val url = legacyNearbyUrl.toHttpUrl().newBuilder()
            .addQueryParameter("location", "-35.2802,149.1310")
            .addQueryParameter("radius", "500")
            .addQueryParameter("type", "cafe")
            .addQueryParameter("opennow", "true")
            .addQueryParameter("key", apiKey)
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .apply { addAndroidHeaders(this, packageName, certSha1) }
            .build()
        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val parsed = runCatching { json.decodeFromString(LegacyEnvelope.serializer(), raw) }.getOrNull()
            val status = parsed?.status
            if (response.isSuccessful && (status == "OK" || status == "ZERO_RESULTS")) return Probe.Ok
            val message = parsed?.errorMessage?.takeIf { it.isNotBlank() }
                ?: status
                ?: "Google rejected the key (${response.code})."
            return Probe.Denied(message, tryLegacy = false)
        }
    }

    private sealed interface Probe {
        data object Ok : Probe
        data class Denied(val message: String, val tryLegacy: Boolean) : Probe
    }

    companion object {
        const val NEW_SEARCH_URL = "https://places.googleapis.com/v1/places:searchText"
        const val LEGACY_NEARBY_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"

        fun newApiNeedsLegacyFallback(httpCode: Int, status: String?, message: String): Boolean {
            if (httpCode == 429 || status == "RESOURCE_EXHAUSTED") return false
            if (httpCode == 403 || status == "PERMISSION_DENIED") return true
            val text = message.lowercase()
            return "has not been used" in text ||
                "is disabled" in text ||
                "service_disabled" in text ||
                "api_key_service_blocked" in text
        }
    }
}

private fun addAndroidHeaders(builder: Request.Builder, packageName: String, certSha1: String) {
    if (packageName.isNotBlank()) builder.header("X-Android-Package", packageName)
    if (certSha1.isNotBlank()) builder.header("X-Android-Cert", certSha1)
}
