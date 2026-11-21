package au.com.uulabs.uufeed.data

enum class PlacesBackend(val storageValue: String, val label: String) {
    NEW("new", "Using Google Places (New)"),
    LEGACY("legacy", "Using Google Places (legacy)"),
    ;

    companion object {
        fun fromStorage(value: String?): PlacesBackend? =
            entries.firstOrNull { it.storageValue == value }
    }
}
