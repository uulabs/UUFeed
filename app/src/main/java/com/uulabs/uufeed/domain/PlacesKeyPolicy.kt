package com.uulabs.uufeed.domain

/**
 * A key typed on the phone wins. The build-time MAPS_API_KEY is only a fallback
 * for people compiling the app themselves.
 */
fun resolvePlacesKey(stored: String?, buildTime: String): String? {
    val user = stored?.trim().orEmpty()
    if (user.isNotEmpty()) return user
    val baked = buildTime.trim()
    return baked.ifEmpty { null }
}

fun maskPlacesKey(key: String?): String? {
    val trimmed = key?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    val tail = trimmed.takeLast(4)
    return "•••• $tail"
}
