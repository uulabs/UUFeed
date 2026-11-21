package au.com.uulabs.uufeed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.com.uulabs.uufeed.domain.Geo
import au.com.uulabs.uufeed.domain.GeoPoint
import au.com.uulabs.uufeed.domain.PlaceCandidate
import au.com.uulabs.uufeed.domain.formatDistanceKm
import au.com.uulabs.uufeed.domain.formatRating
import au.com.uulabs.uufeed.domain.formatReviewCount

@Composable
fun PlaceCard(
    place: PlaceCandidate,
    origin: GeoPoint?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val distance = origin?.let {
        formatDistanceKm(Geo.distanceKm(it, GeoPoint(place.latitude, place.longitude)))
    }
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlaceThumbnail(
                place = place,
                modifier = Modifier.size(96.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                place.typeLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${formatRating(place.rating)} · ${formatReviewCount(place.reviewCount)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = listOfNotNull(place.priceLabel, distance, openLabel(place.openNow)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                place.photos.firstOrNull()?.attribution?.let { credit ->
                    Text(
                        text = credit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceThumbnail(
    place: PlaceCandidate,
    modifier: Modifier = Modifier,
    maxWidthPx: Int = 400,
) {
    val photo = place.photos.firstOrNull()
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (photo == null) {
            Text(
                text = place.name.take(1).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            FeedImage(
                model = photo.mediaUrl(maxWidthPx),
                contentDescription = listOfNotNull(place.name, photo.attribution).joinToString(". "),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

fun openLabel(openNow: Boolean?): String = when (openNow) {
    true -> "Open now"
    false -> "Closed"
    null -> "Hours unconfirmed"
}
