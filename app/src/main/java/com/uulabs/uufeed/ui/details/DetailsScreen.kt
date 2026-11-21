package com.uulabs.uufeed.ui.details

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uulabs.uufeed.domain.Geo
import com.uulabs.uufeed.domain.GeoPoint
import com.uulabs.uufeed.domain.PlaceCandidate
import com.uulabs.uufeed.domain.PlaceDetails
import com.uulabs.uufeed.domain.PlacePhoto
import com.uulabs.uufeed.domain.Review
import com.uulabs.uufeed.domain.formatDistanceKm
import com.uulabs.uufeed.domain.formatRating
import com.uulabs.uufeed.domain.formatReviewCount
import com.uulabs.uufeed.ui.DetailsState
import com.uulabs.uufeed.ui.FeedViewModel
import com.uulabs.uufeed.ui.components.FeedImage
import com.uulabs.uufeed.ui.components.openLabel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    placeId: String,
    listed: PlaceCandidate?,
    detailsState: DetailsState,
    origin: GeoPoint?,
    demo: Boolean,
    onBack: () -> Unit,
    viewModel: FeedViewModel,
) {
    LaunchedEffect(placeId) {
        viewModel.openPlace(placeId)
    }
    val ready = (detailsState as? DetailsState.Ready)?.takeIf { it.summary.id == placeId }
    val failed = (detailsState as? DetailsState.Failed)?.takeIf { it.summary.id == placeId }
    val loading = (detailsState as? DetailsState.Loading)?.takeIf { it.summary.id == placeId }
    val summary = ready?.summary ?: failed?.summary ?: loading?.summary ?: listed
    val loaded = ready?.details
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = loaded?.name ?: summary?.name ?: "Place",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            detailsState is DetailsState.Missing || summary == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("This place isn't in the current list.")
                }
            }
            else -> {
                val photos = loaded?.photos?.ifEmpty { null } ?: summary.photos
                val name = loaded?.name ?: summary.name
                val rating = loaded?.rating ?: summary.rating
                val reviewsCount = loaded?.reviewCount ?: summary.reviewCount
                val price = loaded?.priceLabel ?: summary.priceLabel
                val openNow = loaded?.openNow ?: summary.openNow
                val address = loaded?.address ?: summary.address
                val lat = loaded?.latitude ?: summary.latitude
                val lng = loaded?.longitude ?: summary.longitude
                val distance = origin?.let { formatDistanceKm(Geo.distanceKm(it, GeoPoint(lat, lng))) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .padding(bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (loading != null && loaded == null) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        PhotoPager(photos = photos, placeName = name)
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(name, style = MaterialTheme.typography.headlineSmall)
                            (loaded?.typeLabel ?: summary.typeLabel)?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                Text("${formatRating(rating)} · ${formatReviewCount(reviewsCount)} · $price")
                            }
                            Text(
                                listOfNotNull(distance, openLabel(openNow)).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            loaded?.editorial?.let {
                                Text(it, style = MaterialTheme.typography.bodyLarge)
                            }
                            if (failed != null) {
                                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(failed.message, color = MaterialTheme.colorScheme.onErrorContainer)
                                        TextButton(onClick = { viewModel.openPlace(placeId) }) { Text("Try again") }
                                    }
                                }
                            }
                            ActionRow(
                                lat = lat,
                                lng = lng,
                                placeId = summary.id,
                                phone = loaded?.internationalPhone ?: loaded?.phone,
                                website = loaded?.website,
                            )
                            address?.let {
                                Text(it, style = MaterialTheme.typography.bodyLarge)
                            }
                            HoursBlock(loaded?.weekdayHours.orEmpty())
                            ReviewsBlock(
                                reviews = loaded?.reviews,
                                loading = loading != null && loaded == null,
                                demo = demo,
                            )
                            Text(
                                if (demo) "Demo illustrations and sample reviews. Not from Google." else "Places and reviews from Google.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoPager(photos: List<PlacePhoto>, placeName: String) {
    if (photos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(placeName.take(1).uppercase(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    val pager = rememberPagerState(pageCount = { photos.size })
    Column {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp,
        ) { page ->
            val photo = photos[page]
            FeedImage(
                model = photo.mediaUrl(1200),
                contentDescription = listOfNotNull(placeName, photo.attribution).joinToString(". "),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop,
            )
        }
        photos.getOrNull(pager.currentPage)?.attribution?.let { credit ->
            Text(
                text = credit,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionRow(
    lat: Double,
    lng: Double,
    placeId: String,
    phone: String?,
    website: String?,
) {
    val context = LocalContext.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            val uri = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
                .appendQueryParameter("api", "1")
                .appendQueryParameter("destination", "$lat,$lng")
                .appendQueryParameter("destination_place_id", placeId)
                .build()
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }.onFailure {
                Toast.makeText(context, "Couldn't open Maps.", Toast.LENGTH_SHORT).show()
            }
        }) {
            Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text("Directions")
        }
        if (!phone.isNullOrBlank()) {
            OutlinedButton(onClick = {
                val uri = Uri.parse("tel:${Uri.encode(phone)}")
                runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, uri)) }
            }) {
                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Call")
            }
        }
        if (!website.isNullOrBlank()) {
            OutlinedButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(website))) }
            }) {
                Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Web")
            }
        }
    }
}

@Composable
private fun HoursBlock(hours: List<String>) {
    if (hours.isEmpty()) return
    val today = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Hours", style = MaterialTheme.typography.titleMedium)
        hours.forEach { line ->
            val isToday = line.startsWith(today, ignoreCase = true)
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ReviewsBlock(reviews: List<Review>?, loading: Boolean, demo: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Recent reviews", style = MaterialTheme.typography.titleMedium)
        Text(
            if (demo) {
                "Sample reviews, newest first. Not from Google."
            } else {
                "Newest of the reviews Google returned. The API sends at most five, chosen by relevance, and UU Feed sorts those by time."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            loading && reviews == null -> CircularProgressIndicator()
            reviews.isNullOrEmpty() -> Text("Google didn't return any review text for this place.")
            else -> reviews.forEach { review -> ReviewCard(review) }
        }
    }
}

@Composable
private fun ReviewCard(review: Review) {
    val context = LocalContext.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (review.authorPhotoUrl != null) {
                    FeedImage(
                        model = review.authorPhotoUrl,
                        contentDescription = review.authorName,
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(review.authorName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Column {
                    Text(review.authorName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        listOfNotNull(
                            review.rating?.let { "${formatRating(it)} stars" },
                            review.relativeTime,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (review.text.isNotBlank()) {
                Text(review.text, style = MaterialTheme.typography.bodyMedium)
            }
            review.mapsUri?.let { uri ->
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }
                }) { Text("View on Google") }
            }
        }
    }
}
