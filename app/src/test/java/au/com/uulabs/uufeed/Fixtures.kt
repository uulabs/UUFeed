package au.com.uulabs.uufeed

import au.com.uulabs.uufeed.domain.GeoPoint
import au.com.uulabs.uufeed.domain.PlaceCandidate
import au.com.uulabs.uufeed.domain.PriceLevel

val CANBERRA = GeoPoint(-35.2802, 149.1310)

fun GeoPoint.moveNorth(km: Double): GeoPoint = GeoPoint(latitude + km / 111.195, longitude)

fun samplePlace(
    id: String,
    kmNorth: Double = 0.3,
    rating: Double? = 4.5,
    reviews: Int = 80,
    price: PriceLevel? = PriceLevel.MODERATE,
    open: Boolean? = true,
    type: String? = "cafe",
    origin: GeoPoint = CANBERRA,
    name: String = "Place $id",
): PlaceCandidate {
    val point = origin.moveNorth(kmNorth)
    return PlaceCandidate(
        id = id,
        name = name,
        rating = rating,
        reviewCount = reviews,
        priceLevel = price,
        priceLabel = price?.symbol ?: "Price n/a",
        latitude = point.latitude,
        longitude = point.longitude,
        openNow = open,
        primaryType = type,
        typeLabel = type,
        address = "Canberra ACT",
        photos = emptyList(),
        businessStatus = "OPERATIONAL",
    )
}
