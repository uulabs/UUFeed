package au.com.uulabs.uufeed

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import au.com.uulabs.uufeed.data.ActivePlacesClient
import au.com.uulabs.uufeed.data.JsonConverterFactory
import au.com.uulabs.uufeed.data.PlacesApi
import au.com.uulabs.uufeed.data.PlacesHeaders
import au.com.uulabs.uufeed.data.PlacesKeyStore
import au.com.uulabs.uufeed.data.PlacesKeyTester
import au.com.uulabs.uufeed.data.PlacesRepository
import au.com.uulabs.uufeed.data.RetrofitPlacesClient
import au.com.uulabs.uufeed.demo.demoPlacesClient
import au.com.uulabs.uufeed.domain.SuggestionSession
import au.com.uulabs.uufeed.location.DeviceLocation
import au.com.uulabs.uufeed.ui.FeedViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class UuFeedApp : Application(), SingletonImageLoader.Factory {
    val graph: AppGraph by lazy { AppGraph(this) }

    override fun newImageLoader(context: PlatformContext): ImageLoader = graph.imageLoader
}

class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    val keyStore = PlacesKeyStore(
        context = appContext,
        buildTimeKey = BuildConfig.MAPS_API_KEY,
        signingSha1 = PlacesHeaders.sha1(appContext),
    )
    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(
            PlacesHeaders(
                apiKey = { keyStore.resolve() },
                packageName = appContext.packageName,
                certSha1 = keyStore.signingSha1,
            ),
        )
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val plainHttp = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    val keyTester = PlacesKeyTester(
        http = plainHttp,
        packageName = appContext.packageName,
        certSha1 = keyStore.signingSha1,
        json = json,
    )

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
        client = ActivePlacesClient(
            live = RetrofitPlacesClient(api, { keyStore.resolve() }, json),
            demo = demoPlacesClient(),
        ),
        session = SuggestionSession(),
    )

    val location = DeviceLocation(appContext)

    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FeedViewModel(repository, location, keyStore, keyTester) as T
            }
            error("Unknown model $modelClass")
        }
    }
}
