package com.markvoronin.immichswipe.core

/** Where each action icon sits on the swipe card. */
data class ButtonPositions(
    val fullscreen: IconPosition,
    val immich: IconPosition,
    val rotation: IconPosition,
    val mute: IconPosition,
    val download: IconPosition,
    val share: IconPosition,
    val fullSize: IconPosition
)

/** Which action buttons are shown. */
data class ButtonVisibility(
    val fullscreen: Boolean,
    val immich: Boolean,
    val rotation: Boolean,
    val mute: Boolean,
    val download: Boolean,
    val share: Boolean,
    val fullSize: Boolean,
    val favorite: Boolean,
    val lock: Boolean,
    val archive: Boolean,
    val swipeButtons: Boolean
)

/** Behaviour switches of the swipe screen. */
data class SwipeBehavior(
    val playback: PlaybackBehavior,
    val autoNextOnFavorite: Boolean,
    val syncLocalDeletion: Boolean,
    val trashLocalDeletion: Boolean,
    val sortOrder: SortOrder,
    val tapToSwipe: Boolean
)

/** All user settings the swipe screen reacts to, as one typed value. */
data class SwipeSettings(
    val positions: ButtonPositions,
    val visibility: ButtonVisibility,
    val behavior: SwipeBehavior
)
