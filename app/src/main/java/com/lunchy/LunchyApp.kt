package com.lunchy

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import com.lunchy.data.JsonConverterFactory
import com.lunchy.data.PlacesApi
import com.lunchy.data.PlacesHeaders
import com.lunchy.data.PlacesRepository
import com.lunchy.data.RetrofitPlacesClient
import com.lunchy.domain.SuggestionSession
import com.lunchy.location.DeviceLocation
import com.lunchy.ui.LunchViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class LunchyApp : Application(), SingletonImageLoader.Factory {
    val graph: AppGraph by lazy { AppGraph(this) }

    override fun newImageLoader(context: PlatformContext): ImageLoader = graph.imageLoader
}

class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    private val apiKey = BuildConfig.MAPS_API_KEY
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(
            PlacesHeaders(
                apiKey = apiKey,
                packageName = appContext.packageName,
                certSha1 = PlacesHeaders.sha1(appContext),
            ),
        )
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    val imageLoader: ImageLoader = ImageLoader.Builder(appContext)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = okHttp))
        }
        .diskCachePolicy(CachePolicy.DISABLED)
        .build()

    private val api: PlacesApi = Retrofit.Builder()
        .baseUrl("https://places.googleapis.com/")
        .client(okHttp)
        .addConverterFactory(
            JsonConverterFactory(json, "application/json".toMediaType()),
        )
        .build()
        .create(PlacesApi::class.java)

    val repository = PlacesRepository(
        client = RetrofitPlacesClient(api, apiKey, json),
        session = SuggestionSession(),
    )

    val location = DeviceLocation(appContext)

    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LunchViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LunchViewModel(repository, location) as T
            }
            error("Unknown model $modelClass")
        }
    }
}
