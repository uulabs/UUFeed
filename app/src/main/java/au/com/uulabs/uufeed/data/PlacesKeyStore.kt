package au.com.uulabs.uufeed.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import au.com.uulabs.uufeed.domain.maskPlacesKey
import au.com.uulabs.uufeed.domain.resolvePlacesKey

data class PlacesKeyStatus(
    val activeKey: String?,
    val userKeyPresent: Boolean,
    val usingBuildKey: Boolean,
    val buildKeyPresent: Boolean,
    val maskedUserKey: String?,
)

class PlacesKeyStore(
    context: Context,
    private val buildTimeKey: String,
    val signingSha1: String,
) {
    private val prefs: SharedPreferences = encryptedPrefs(context)

    fun readUserKey(): String? = prefs.getString(KEY, null)

    fun status(): PlacesKeyStatus {
        val stored = readUserKey()
        val active = resolvePlacesKey(stored, buildTimeKey)
        val userPresent = !stored.isNullOrBlank()
        return PlacesKeyStatus(
            activeKey = active,
            userKeyPresent = userPresent,
            usingBuildKey = !userPresent && !active.isNullOrBlank(),
            buildKeyPresent = buildTimeKey.isNotBlank(),
            maskedUserKey = maskPlacesKey(stored),
        )
    }

    fun resolve(): String = status().activeKey.orEmpty()

    fun save(raw: String) {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "Paste a key first." }
        prefs.edit().putString(KEY, trimmed).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private fun encryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private companion object {
        const val FILE = "uufeed_secrets"
        const val KEY = "places_api_key"
    }
}
