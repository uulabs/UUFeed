# Lunchy

Lunchy picks five places that are open right now, near you, and mixes them up instead of always handing back the five most famous. It is a native Android app (Kotlin, Jetpack Compose, Material 3) aimed at someone standing in Canberra who wants lunch, but the search works from any location. Distances are kilometres. Prices are shown in whatever currency Google sends, which in Australia is usually AUD.

## What it does

- Uses the device location after you allow it. If you say no, type a suburb or address (`Braddon, Canberra`). When a name matches several places, an Australian match is preferred.
- Filters on the main screen: radius from 0.5 km to 25 km, minimum rating (any / 3+ / 3.5+ / 4+ / 4.5+), and a budget that maps to Google's price levels. `$` means cheap eats (free or inexpensive). `$$`, `$$$`, and `$$$$` are ceilings. An optional craving field is sent as the text query.
- Asks Google for places that are open now, and drops anything explicitly closed.
- Shows five suggestions: name, rating, review count, price, distance, open status, and a photo. The five are spread across place types and are not a straight cut of the most prominent results.
- A place screen has photos, address, phone, website, today's hours, a Directions button that opens Google Maps, and up to five reviews (author, rating, relative time, text, author photo when Google sends one).
- **Five more** returns five places that have not been shown yet in this app process. The exclusion set is only in memory. Force-stopping Lunchy, or the system killing the process, starts a fresh list. Changing filters does not bring a place back once it has been shown.
- If the current pool runs out, Lunchy asks for the next page, then looks further out (up to 25 km) and says so. When nothing new is left, it says that too.

## Demo mode

Debug builds have a **Demo places** switch on the search screen. It serves invented Civic lunch spots, sample reviews, and bundled illustrations so the screens can be checked without a Places key. The switch is not in the release source set, and release builds cannot turn it on. Leave it off when you have a real key. Debug builds can also save a window drawing with `adb shell am broadcast -n com.lunchy/.demo.DebugCaptureReceiver -a com.lunchy.CAPTURE --es name shot`, which is useful when an emulator's GPU framebuffer is blank.

## Requirements

- JDK 17 or newer (the project compiles to JVM 17)
- Android SDK platform 37 and build-tools 36.0.0
- Android Gradle Plugin 9.5 (Gradle 9.6), which current Android Studio releases can open
- A device or emulator on Android 8.0 (API 26) or newer
- A Google Cloud project with billing, and a Places API (New) key

Android Studio (a 2026 release that understands Android Gradle Plugin 9.5) is the easiest way to run it. The command line works too.

## API key

Lunchy calls the [Places API (New)](https://developers.google.com/maps/documentation/places/web-service/op-overview) over HTTPS. The legacy Places API is not used.

1. In [Google Cloud Console](https://console.cloud.google.com/), create or pick a project and attach a billing account. Places will not answer without billing, even inside the free monthly cap.
2. Enable **Places API (New)**. The legacy "Places API" product is a different API and will not serve these calls.
3. Create an API key. Restrict the API to **Places API (New)** only.
4. Restrict the key to Android apps:
   - Package name: `com.lunchy`
   - SHA-1: the certificate that signs the build you install
5. For a debug build on your machine:

   ```bash
   ./gradlew signingReport
   ```

   Copy the SHA-1 under `Variant: debug`. Each computer's debug keystore (`~/.android/debug.keystore`) is different, so a key restricted to one laptop will 403 on another until you add that SHA-1.
6. Put the key in `local.properties` (this file is gitignored):

   ```properties
   sdk.dir=/path/to/Android/sdk
   MAPS_API_KEY=your_key_here
   ```

   `local.properties.example` is the template. You can also export `MAPS_API_KEY` in the environment; `local.properties` wins if both are set.
7. Rebuild and reinstall. The key is baked into `BuildConfig` at compile time. Changing the file does nothing until the next build. The debug APK contains the key. Do not ship that APK publicly, and do not commit a real key.

Lunchy sends `X-Goog-Api-Key`, plus `X-Android-Package` and `X-Android-Cert` (SHA-1, uppercase, no colons). Those headers are what make an Android-restricted key work for a direct REST call. Photo requests go to `places.googleapis.com` as well, with the same headers, so Coil does not put the key in the image URL.

For a quick local experiment you can leave the key restricted only by API (Places API (New)) and skip the Android app restriction. Turn the app restriction on before anyone else has the APK.

### What a search costs

Prices below are Google's published first-band list prices in USD, after the free monthly cap, as of late 2026. Check the [Maps Platform pricing list](https://developers.google.com/maps/billing-and-pricing/pricing) before you rely on them. The field mask decides the SKU: one call is billed at the dearest field in the mask.

| Call | When | SKU | Free / month | Then, per 1,000 |
| --- | --- | --- | --- | --- |
| Text Search | Each results fetch | Enterprise (rating, price, open-now hours) | 1,000 | $35 |
| Place Details | Opening a place, because reviews are requested | Enterprise + Atmosphere | 1,000 | $25 |
| Place Photo | Each photo Coil actually loads | Place Details Photos | 1,000 | $7 |

A normal "find five" is one Text Search. **Five more** is free of Text Search charges when the twenty places already fetched still have unseen matches. If they don't, Lunchy may request up to three more pages at the same radius, then a wider search, and it stops after four Places calls per tap. Opening a place is one Details call. Five thumbnails plus a few detail photos are separate photo charges. Reviews are not requested on the search, so the search stays off the Atmosphere SKU.

Lunchy does not write place details or photo bytes to disk.

## Build

```bash
cp local.properties.example local.properties
# edit sdk.dir and MAPS_API_KEY

./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The debug APK is:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Unit tests cover filtering (radius, rating, budget, open-now), the in-memory exclusion set, the "not just the top five" picker, paging and widening, review sorting, and JSON mapping. They do not call Google.

GitHub Actions (`.github/workflows/android.yml`) runs `assembleDebug` and `testDebugUnitTest` on Ubuntu with JDK 17. CI has no API key; the APK builds with an empty key and the app explains that on first search.

## Install the debug APK

USB debugging on, then:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy `app-debug.apk` to the phone and open it. Android will ask you to allow installs from that app (Files, Chrome, or whichever one you used). If a previous Lunchy is signed with a different key, uninstall it first.

## Layout

MVVM. The activity holds one `LunchViewModel`. `PlacesRepository` owns the session and talks to Places through Retrofit and kotlinx.serialization. Filtering, the picker, and the exclusion set are plain Kotlin with no Android dependencies, which is what the unit tests run. Images use Coil 3. Hilt is not used; `LunchyApp` builds the graph by hand.

Text Search is the search call because it supports `openNow`, `minRating`, `priceLevels`, and a page token. Nearby Search (New) has no open-now or rating filter and no pagination. `locationBias` is a hint, not a fence (Text Search's hard restriction is a rectangle, not a circle), so Lunchy drops places outside the chosen radius itself.

## Limitations

- Google returns **at most five reviews**, ordered by relevance. There is no "newest first" parameter. Lunchy sorts those five by `publishTime`. A review with no timestamp sinks to the bottom. This is not the five newest reviews ever written, only the newest of the five Google chose to send. Review photos are not in the API; author profile photos are shown when present.
- `openNow` is Google's hours. A place with no hours flag is kept, because the query already asked for places that are open. A place Google marks closed is removed.
- A budget other than **Any** hides places with no price level. `$$$$` still requires Google to have sent a level. When Google sends a `priceRange`, it is shown next to the dollar symbols (often `A$20–A$40`).
- The radius is a straight line, not a walking or driving route. Widening steps through 1, 2, 3, 5, 8, 12, 18, and 25 km and stops at 25 km.
- The exclusion list lasts for the process only. It is not in DataStore or a database. Rotation keeps it. Swiping the app away can clear it.
- The device geocoder (not a billed Geocoding API call) turns a typed suburb into coordinates. If that geocoder is unavailable, the search cannot start from text.
- Directions needs something that can open `https://www.google.com/maps/dir/`. Call needs a dialer.
- Photo author names are shown when Google requires attribution. There is also a "Places and reviews from Google" line. If you ship this, follow the [Google Maps Platform attribution rules](https://developers.google.com/maps/documentation/places/web-service/policies) and add the official lockup if your agreement requires the logo.
- Fraunces and Outfit are included under the SIL Open Font License. See `third_party/fonts/`.
