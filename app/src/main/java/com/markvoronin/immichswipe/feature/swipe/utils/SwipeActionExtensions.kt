package com.markvoronin.immichswipe.feature.swipe.utils

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SwipeAction

val SwipeAction.icon: ImageVector
    get() = when (this) {
        SwipeAction.KEEP -> Icons.Default.Check
        SwipeAction.DELETE -> Icons.Default.Delete
        SwipeAction.ARCHIVE -> Icons.Default.Archive
        SwipeAction.LOCK -> Icons.Default.Lock
        SwipeAction.FAVORITE -> Icons.Default.Favorite
        SwipeAction.UNDO -> Icons.AutoMirrored.Filled.Undo
        SwipeAction.DETAILS -> Icons.Default.Info
        SwipeAction.ALBUM -> Icons.Default.LibraryAdd
    }

@get:StringRes
val SwipeAction.labelRes: Int
    get() = when (this) {
        SwipeAction.KEEP -> R.string.swipe_keep
        SwipeAction.DELETE -> R.string.swipe_delete
        SwipeAction.ARCHIVE -> R.string.swipe_archive
        SwipeAction.LOCK -> R.string.swipe_locked
        SwipeAction.FAVORITE -> R.string.swipe_label_favorite
        SwipeAction.UNDO -> R.string.swipe_undo
        SwipeAction.DETAILS -> R.string.swipe_details
        SwipeAction.ALBUM -> R.string.swipe_label_album
    }

/** The label shown over the card while it is dragged towards this action. */
@get:StringRes
val SwipeAction.indicatorLabelRes: Int
    get() = when (this) {
        SwipeAction.KEEP -> R.string.swipe_keep_upper
        SwipeAction.DELETE -> R.string.swipe_delete_upper
        SwipeAction.ARCHIVE -> R.string.swipe_archive_upper
        SwipeAction.LOCK -> R.string.swipe_locked_upper
        SwipeAction.FAVORITE -> R.string.swipe_favorite_upper
        SwipeAction.UNDO -> R.string.swipe_undo_upper
        SwipeAction.DETAILS -> R.string.swipe_details // opens a panel, so it is never shown as a badge
        SwipeAction.ALBUM -> R.string.swipe_album_upper
    }

@Composable
fun SwipeAction.indicatorColor(): Color = when (this) {
    SwipeAction.KEEP -> KeepGreen
    SwipeAction.DELETE -> MaterialRed
    SwipeAction.ARCHIVE -> MaterialTheme.colorScheme.primary
    SwipeAction.LOCK -> MaterialTheme.colorScheme.tertiary
    SwipeAction.FAVORITE -> Color.Red
    SwipeAction.UNDO -> MaterialTheme.colorScheme.primary
    SwipeAction.DETAILS -> MaterialTheme.colorScheme.primary
    SwipeAction.ALBUM -> MaterialTheme.colorScheme.secondary
}
