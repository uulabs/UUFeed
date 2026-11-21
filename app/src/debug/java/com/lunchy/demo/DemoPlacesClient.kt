package com.lunchy.demo

import com.lunchy.data.PlacesClient
import com.lunchy.data.PlacesException
import com.lunchy.domain.Geo
import com.lunchy.domain.GeoPoint
import com.lunchy.domain.Money
import com.lunchy.domain.PlaceCandidate
import com.lunchy.domain.PlaceDetails
import com.lunchy.domain.PlacePhoto
import com.lunchy.domain.PlacesQuery
import com.lunchy.domain.PriceLevel
import com.lunchy.domain.PriceQuote
import com.lunchy.domain.Review
import com.lunchy.domain.SearchPage
import com.lunchy.domain.formatPrice
import com.lunchy.domain.newestReviews
import java.time.Instant

/**
 * Invented Canberra lunch spots. Filtered the same way Text Search would:
 * open now, distance, rating, price level, and the craving words.
 */
class DemoPlacesClient : PlacesClient {
    override suspend fun search(query: PlacesQuery): SearchPage {
        val origin = query.origin
        val radiusKm = query.radiusMeters / 1000.0
        val places = DemoCatalog.all.map { it.summary }.filter { place ->
            matches(place, query, origin, radiusKm)
        }
        return SearchPage(places, nextPageToken = null)
    }

    override suspend fun details(placeId: String): PlaceDetails {
        return DemoCatalog.all.firstOrNull { it.summary.id == placeId }?.details
            ?: throw PlacesException("No demo place with that id.")
    }

    private fun matches(place: PlaceCandidate, query: PlacesQuery, origin: GeoPoint, radiusKm: Double): Boolean {
        if (place.openNow == false) return false
        val distance = Geo.distanceKm(origin, GeoPoint(place.latitude, place.longitude))
        if (distance > radiusKm + 0.02) return false
        val min = query.minRating
        if (min != null && (place.rating == null || place.rating < min)) return false
        val levels = query.priceLevels
        if (levels != null && place.priceLevel?.apiValue !in levels) return false
        return matchesKeyword(place, query.textQuery)
    }

    private fun matchesKeyword(place: PlaceCandidate, textQuery: String): Boolean {
        if (textQuery.equals("restaurants cafes", ignoreCase = true)) return true
        val haystack = listOfNotNull(place.name, place.typeLabel, place.primaryType, place.address)
            .joinToString(" ")
            .lowercase()
        return textQuery.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }.all { token ->
            token in haystack
        }
    }
}

object DemoCatalog {
    val all: List<DemoPlace> = listOf(
        spot(
            id = "demo-lonsdale-loaf",
            name = "Lonsdale Loaf",
            type = "bakery",
            typeLabel = "Bakery",
            northKm = 0.7,
            eastKm = 0.3,
            rating = 4.6,
            reviewCount = 412,
            price = PriceLevel.INEXPENSIVE,
            low = 8,
            high = 16,
            address = "12 Lonsdale Street, Braddon ACT 2612",
            phone = "02 6100 0142",
            blurb = "Seeded loaves and a short lunch menu, a few minutes north of Civic.",
            photo = "demo_lonsdale",
            hours = weekday("7:00 AM – 3:00 PM", "8:00 AM – 2:00 PM"),
        ),
        spot(
            id = "demo-haig-lane",
            name = "Haig Lane Coffee",
            type = "cafe",
            typeLabel = "Cafe",
            northKm = 0.2,
            eastKm = -0.1,
            rating = 4.4,
            reviewCount = 890,
            price = PriceLevel.INEXPENSIVE,
            low = 5,
            high = 14,
            address = "8 Haig Lane, Civic ACT 2601",
            phone = "02 6100 0188",
            blurb = "Small cups, big window, and a counter that turns over fast at noon.",
            photo = "demo_haig",
            hours = weekday("7:00 AM – 4:00 PM", "8:00 AM – 3:00 PM"),
        ),
        spot(
            id = "demo-bunda-bao",
            name = "Bunda Bao",
            type = "meal_takeaway",
            typeLabel = "Takeaway",
            northKm = 0.15,
            eastKm = 0.45,
            rating = 4.5,
            reviewCount = 230,
            price = PriceLevel.INEXPENSIVE,
            low = 12,
            high = 18,
            address = "40 Bunda Street, Canberra ACT 2601",
            phone = "02 6100 0190",
            blurb = "Steamed buns and a short list of sides, built for a quick Civic lunch.",
            photo = "demo_bunda",
            hours = weekday("11:00 AM – 3:00 PM", "11:00 AM – 4:00 PM"),
        ),
        spot(
            id = "demo-london-ramen",
            name = "London Circuit Ramen",
            type = "ramen_restaurant",
            typeLabel = "Ramen restaurant",
            northKm = -0.25,
            eastKm = 0.35,
            rating = 4.7,
            reviewCount = 640,
            price = PriceLevel.MODERATE,
            low = 18,
            high = 28,
            address = "1 London Circuit, Canberra ACT 2601",
            phone = "02 6100 0211",
            blurb = "Rich broth and a queue that moves. Worth it if you have forty minutes.",
            photo = "demo_ramen",
            hours = weekday("11:30 AM – 2:30 PM", "12:00 PM – 3:00 PM"),
        ),
        spot(
            id = "demo-allara-thai",
            name = "Allara Thai",
            type = "thai_restaurant",
            typeLabel = "Thai restaurant",
            northKm = 0.4,
            eastKm = 0.85,
            rating = 4.3,
            reviewCount = 510,
            price = PriceLevel.MODERATE,
            low = 20,
            high = 34,
            address = "22 Allara Street, Canberra ACT 2601",
            phone = "02 6100 0244",
            blurb = "Weekday curries and a salad that actually tastes of lime.",
            photo = "demo_thai",
            hours = weekday("11:30 AM – 2:30 PM", "12:00 PM – 3:00 PM"),
        ),
        spot(
            id = "demo-acton-grill",
            name = "Acton Grill",
            type = "restaurant",
            typeLabel = "Restaurant",
            northKm = 0.1,
            eastKm = -1.3,
            rating = 4.2,
            reviewCount = 188,
            price = PriceLevel.EXPENSIVE,
            low = 36,
            high = 62,
            address = "15 Acton Peninsula, Acton ACT 2601",
            phone = "02 6100 0280",
            blurb = "A slower lunch with a view back toward the lake.",
            photo = "demo_acton",
            hours = weekday("12:00 PM – 3:00 PM", "12:00 PM – 3:00 PM"),
        ),
        spot(
            id = "demo-glebe-greens",
            name = "Glebe Park Greens",
            type = "vegetarian_restaurant",
            typeLabel = "Vegetarian restaurant",
            northKm = -0.55,
            eastKm = 0.2,
            rating = 4.6,
            reviewCount = 276,
            price = PriceLevel.MODERATE,
            low = 16,
            high = 26,
            address = "4 Akuna Street, Canberra ACT 2601",
            phone = "02 6100 0312",
            blurb = "Grain bowls and a shady courtyard when the wind is down.",
            photo = "demo_greens",
            hours = weekday("8:00 AM – 3:00 PM", "8:00 AM – 3:00 PM"),
        ),
        spot(
            id = "demo-mort-pizza",
            name = "Mort Street Pizza",
            type = "pizza_restaurant",
            typeLabel = "Pizza restaurant",
            northKm = 1.15,
            eastKm = 0.55,
            rating = 4.1,
            reviewCount = 960,
            price = PriceLevel.MODERATE,
            low = 18,
            high = 32,
            address = "88 Mort Street, Braddon ACT 2612",
            phone = "02 6100 0330",
            blurb = "Wood-fired rounds by the slice at lunch, whole pies after two.",
            photo = "demo_pizza",
            hours = weekday("11:30 AM – 9:00 PM", "12:00 PM – 9:00 PM"),
        ),
        spot(
            id = "demo-city-dumplings",
            name = "City Walk Dumplings",
            type = "chinese_restaurant",
            typeLabel = "Chinese restaurant",
            northKm = 0.05,
            eastKm = 0.15,
            rating = 4.8,
            reviewCount = 1204,
            price = PriceLevel.INEXPENSIVE,
            low = 14,
            high = 22,
            address = "City Walk, Canberra ACT 2601",
            phone = "02 6100 0348",
            blurb = "Twelve to a basket and a chilli oil that people ask to take away.",
            photo = "demo_dumplings",
            hours = weekday("11:00 AM – 3:00 PM", "11:00 AM – 4:00 PM"),
        ),
        spot(
            id = "demo-newacton",
            name = "NewActon Cellar",
            type = "bar",
            typeLabel = "Bar",
            northKm = -0.35,
            eastKm = -1.15,
            rating = 4.4,
            reviewCount = 340,
            price = PriceLevel.EXPENSIVE,
            low = 24,
            high = 44,
            address = "25 Childers Street, Canberra ACT 2601",
            phone = "02 6100 0366",
            blurb = "A proper plate and a glass, if lunch is allowed to take an hour.",
            photo = "demo_cellar",
            hours = weekday("12:00 PM – 10:00 PM", "12:00 PM – 11:00 PM"),
        ),
        spot(
            id = "demo-elouera",
            name = "Elouera Street Kitchen",
            type = "cafe",
            typeLabel = "Cafe",
            northKm = 1.5,
            eastKm = -0.4,
            rating = 4.0,
            reviewCount = 97,
            price = PriceLevel.INEXPENSIVE,
            low = 9,
            high = 19,
            address = "6 Elouera Street, Braddon ACT 2612",
            phone = "02 6100 0381",
            blurb = "Toasties, a soup of the day, and tables that empty by half past one.",
            photo = "demo_elouera",
            hours = weekday("7:30 AM – 3:00 PM", "8:00 AM – 2:30 PM"),
        ),
        spot(
            id = "demo-capital-noodles",
            name = "Capital Noodles",
            type = "noodle_shop",
            typeLabel = "Noodle shop",
            northKm = -0.9,
            eastKm = 1.05,
            rating = 4.5,
            reviewCount = 455,
            price = PriceLevel.INEXPENSIVE,
            low = 13,
            high = 21,
            address = "18 East Row, Canberra ACT 2601",
            phone = "02 6100 0402",
            blurb = "Hand-pulled noodles in a room that smells like stock by eleven.",
            photo = "demo_noodles",
            hours = weekday("11:00 AM – 8:00 PM", "11:00 AM – 8:30 PM"),
        ),
        spot(
            id = "demo-old-canteen",
            name = "Old Civic Canteen",
            type = "restaurant",
            typeLabel = "Restaurant",
            northKm = 0.3,
            eastKm = 0.2,
            rating = 3.4,
            reviewCount = 40,
            price = PriceLevel.INEXPENSIVE,
            low = 10,
            high = 18,
            address = "3 Mort Street, Canberra ACT 2601",
            phone = "02 6100 0101",
            blurb = "Closed for a refit. It should not appear in an open-now search.",
            photo = "demo_closed",
            hours = weekday("Closed", "Closed"),
            openNow = false,
        ),
    )

    private fun weekday(weekday: String, weekend: String): List<String> = listOf(
        "Monday: $weekday",
        "Tuesday: $weekday",
        "Wednesday: $weekday",
        "Thursday: $weekday",
        "Friday: $weekday",
        "Saturday: $weekend",
        "Sunday: $weekend",
    )

    private fun spot(
        id: String,
        name: String,
        type: String,
        typeLabel: String,
        northKm: Double,
        eastKm: Double,
        rating: Double,
        reviewCount: Int,
        price: PriceLevel,
        low: Long,
        high: Long,
        address: String,
        phone: String,
        blurb: String,
        photo: String,
        hours: List<String>,
        openNow: Boolean = true,
    ): DemoPlace {
        val point = GeoPoint(
            DemoMode.origin.latitude + northKm / 111.195,
            DemoMode.origin.longitude + eastKm / 90.74,
        )
        val quote = PriceQuote(Money("AUD", low, 0), Money("AUD", high, 0))
        val priceLabel = formatPrice(price, quote)
        val photos = listOf(
            PlacePhoto("android.resource://com.lunchy/drawable/$photo", null),
        )
        val summary = PlaceCandidate(
            id = id,
            name = name,
            rating = rating,
            reviewCount = reviewCount,
            priceLevel = price,
            priceLabel = priceLabel,
            latitude = point.latitude,
            longitude = point.longitude,
            openNow = openNow,
            primaryType = type,
            typeLabel = typeLabel,
            address = address,
            photos = photos,
            businessStatus = if (openNow) "OPERATIONAL" else "CLOSED_TEMPORARILY",
        )
        val details = PlaceDetails(
            id = id,
            name = name,
            address = address,
            phone = phone,
            internationalPhone = "+61 ${phone.removePrefix("0")}",
            website = "https://example.com/lunchy-demo/$id",
            mapsUri = null,
            rating = rating,
            reviewCount = reviewCount,
            priceLabel = priceLabel,
            priceLevel = price,
            openNow = openNow,
            weekdayHours = hours,
            photos = photos,
            reviews = newestReviews(sampleReviews()),
            editorial = blurb,
            typeLabel = typeLabel,
            latitude = point.latitude,
            longitude = point.longitude,
        )
        return DemoPlace(summary, details)
    }

    private fun sampleReviews(): List<Review> {
        val authors = listOf(
            Triple("Mina Chen", "demo_avatar_mina", 5.0),
            Triple("Owen Blake", "demo_avatar_owen", 4.0),
            Triple("Priya Nair", "demo_avatar_priya", 5.0),
            Triple("Sam Keller", "demo_avatar_sam", 4.0),
            Triple("Jules Okonkwo", "demo_avatar_jules", 3.0),
        )
        val lines = listOf(
            "Came in on a Saturday and still got a table by the window. The lunch plate was exactly what I wanted.",
            "Quick, generous, and the staff remembered a takeaway lid without being asked.",
            "Best thing I ate in Civic this month. I would walk back from the bus interchange for it.",
            "Solid, not flashy. Portion was right and the bill matched the board.",
            "A bit loud at 12:30, but the food landed hot and the coffee was properly made.",
        )
        val times = listOf(
            "2026-11-18T01:10:00Z" to "3 days ago",
            "2026-11-09T04:20:00Z" to "2 weeks ago",
            "2026-10-22T06:00:00Z" to "a month ago",
            "2026-09-02T02:15:00Z" to "2 months ago",
            "2026-06-14T05:40:00Z" to "5 months ago",
        )
        return authors.mapIndexed { index, (author, avatar, stars) ->
            Review(
                authorName = author,
                authorPhotoUrl = "android.resource://com.lunchy/drawable/$avatar",
                rating = stars,
                relativeTime = times[index].second,
                text = lines[index],
                publishedAt = Instant.parse(times[index].first),
                mapsUri = null,
            )
        }.let { reviews ->
            // Shuffle the written order so newest-first sorting has work to do.
            listOf(reviews[3], reviews[1], reviews[4], reviews[0], reviews[2])
        }
    }
}

data class DemoPlace(
    val summary: PlaceCandidate,
    val details: PlaceDetails,
)
