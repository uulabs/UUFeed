package com.uulabs.uufeed.domain

import java.time.Instant

/**
 * Places API (New) returns at most five reviews, ordered by relevance, and has
 * no newest-first parameter. Sort the batch it did return by publishTime.
 * Reviews with no timestamp sink to the end and keep their relative order.
 */
fun newestReviews(reviews: List<Review>, limit: Int = 5): List<Review> =
    reviews
        .sortedByDescending { it.publishedAt ?: Instant.EPOCH }
        .take(limit)
