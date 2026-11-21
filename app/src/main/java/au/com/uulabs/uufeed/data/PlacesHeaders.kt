package au.com.uulabs.uufeed.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest

class PlacesHeaders(
    private val apiKey: () -> String,
    private val packageName: String,
    private val certSha1: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.url.host.equals("places.googleapis.com", ignoreCase = true)) {
            return chain.proceed(request)
        }
        val builder = request.newBuilder()
        val key = apiKey().trim()
        if (key.isNotBlank()) builder.header("X-Goog-Api-Key", key)
        if (packageName.isNotBlank()) builder.header("X-Android-Package", packageName)
        if (certSha1.isNotBlank()) builder.header("X-Android-Cert", certSha1)
        return chain.proceed(builder.build())
    }

    companion object {
        fun sha1(context: Context): String = runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= 28) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES,
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES,
                )
            }
            val signatures = if (Build.VERSION.SDK_INT >= 28) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            val signature = signatures?.firstOrNull() ?: return ""
            val digest = MessageDigest.getInstance("SHA-1").digest(signature.toByteArray())
            digest.joinToString("") { byte -> "%02X".format(byte) }
        }.getOrDefault("")
    }
}
