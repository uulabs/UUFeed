package au.com.uulabs.uufeed.data

import au.com.uulabs.uufeed.data.dto.GoogleErrorEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

sealed interface KeyTestResult {
    data class Success(val message: String) : KeyTestResult
    data class Failed(val message: String) : KeyTestResult
}

/**
 * One Text Search that asks only for place IDs. That field mask stays on the
 * free Places Essentials (IDs Only) tier.
 */
class PlacesKeyTester(
    private val http: OkHttpClient,
    private val packageName: String,
    private val certSha1: String,
    private val json: Json,
) {
    suspend fun test(apiKey: String): KeyTestResult = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            return@withContext KeyTestResult.Failed("Paste a key first.")
        }
        val body = """{"textQuery":"cafe","pageSize":1,"regionCode":"AU"}"""
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://places.googleapis.com/v1/places:searchText")
            .header("X-Goog-Api-Key", trimmed)
            .header("X-Goog-FieldMask", "places.id")
            .header("X-Android-Package", packageName)
            .header("X-Android-Cert", certSha1)
            .post(body)
            .build()
        try {
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    KeyTestResult.Success(
                        "Key works. Google accepted it. This check only asks for place IDs, which stays on the free Essentials tier.",
                    )
                } else {
                    KeyTestResult.Failed(errorMessage(raw, response.code))
                }
            }
        } catch (_: IOException) {
            KeyTestResult.Failed("Couldn't reach Google. Check your connection and try again.")
        }
    }

    private fun errorMessage(raw: String, code: Int): String {
        val parsed = runCatching { json.decodeFromString(GoogleErrorEnvelope.serializer(), raw) }.getOrNull()
        val message = parsed?.error?.message ?: "Google rejected the key ($code)."
        val hint = when (code) {
            400 -> " Enable Places API (New) on this Google Cloud project. The older Places API is a different product."
            403 -> " Check that billing is on, Places API (New) is enabled, and any Android restriction uses package au.com.uulabs.uufeed plus the SHA-1 shown on this screen."
            429 -> " The quota for this key is used up."
            else -> ""
        }
        return message + hint
    }
}
