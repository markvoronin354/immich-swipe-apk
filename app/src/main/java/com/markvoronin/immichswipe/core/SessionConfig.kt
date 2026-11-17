package com.markvoronin.immichswipe.core

data class SessionConfig(
    val baseUrl: String,
    val apiKey: String,
    val userId: String = "",
    // Extra headers for a proxy in front of the server; sent to this server only.
    val headers: Map<String, String> = emptyMap(),
)


enum class PlaybackBehavior {
    PAUSE_OTHERS,
    IGNORE
}


enum class IconPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

enum class ButtonStackOrientation {
    VERTICAL,
    HORIZONTAL
}


enum class DoubleTapAction {
    FULLSCREEN,
    FAVORITE
}

/**
 * What a swipe does. FAVORITE favorites the asset, then keeps it so the deck moves on.
 * UNDO only makes sense for a swipe down, which is what undoes by default.
 * DETAILS only makes sense for a swipe up, which is what opens the metadata panel by default.
 */
enum class SwipeAction {
    KEEP,
    DELETE,
    ARCHIVE,
    LOCK,
    FAVORITE,
    UNDO,
    DETAILS
}

/** The reorderable middle buttons of the swipe toolbar, in their default order. */
enum class ToolbarButton {
    ARCHIVE,
    FAVORITE,
    ALBUM,
    LOCK
}

enum class SortOrder {
    CHRONOLOGICAL_DESC,
    CHRONOLOGICAL_ASC,
    SHUFFLED,
    SIZE_DESC,
    SIZE_ASC,
    TYPE_VIDEO_FIRST,
    TYPE_PHOTO_FIRST,
    TYPE_VIDEO_FIRST_ASC,
    TYPE_PHOTO_FIRST_ASC,
    TYPE_VIDEO_FIRST_SHUFFLED,
    TYPE_PHOTO_FIRST_SHUFFLED
}


enum class SortCategory {
    TIME,
    SIZE,
    TYPE
}


enum class ReminderInterval(val minDays: Long, val maxDays: Long) {
    OFTEN(7, 14),
    RARELY(21, 35),
    CUSTOM(0, 0);

    fun getMinDays(customMin: Long = 3): Long = if (this == CUSTOM) customMin else minDays
    fun getMaxDays(customMax: Long = 7): Long = if (this == CUSTOM) customMax else maxDays
}
