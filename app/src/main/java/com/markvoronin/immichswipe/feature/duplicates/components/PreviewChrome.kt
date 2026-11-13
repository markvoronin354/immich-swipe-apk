package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.common.FavoriteIcon
import com.markvoronin.immichswipe.feature.common.bounce
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision
import com.markvoronin.immichswipe.feature.swipe.components.connectedTileShape
import kotlinx.coroutines.launch

// Small tiles keep the chrome quiet around the photo; the buttons still get a 48dp touch target.
private val ControlTileHeight = 40.dp

/** Containerless like the swipe fullscreen's top bar: back on the left, then the name, favourite and info. */
@Composable
internal fun PreviewTopBar(
    asset: Asset,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val motionScheme = MaterialTheme.motionScheme
    var isFileNameExpanded by remember(asset.id) { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isFileNameExpanded = !isFileNameExpanded
                }
        ) {
            val sizeStr = asset.exifInfo?.fileSizeInBytes?.let { formatSizeStr(it) } ?: ""
            val originalName = asset.originalFileName ?: stringResource(R.string.duplicates_photo_fallback)

            val displayedName = if (isFileNameExpanded) {
                originalName
            } else {
                formatMiddleEllipsisFileName(originalName, maxLength = 22)
            }

            Text(
                text = displayedName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = if (isFileNameExpanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis
            )
            if (sizeStr.isNotEmpty()) {
                Text(
                    text = sizeStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Same bounce and burst as the swipe fullscreen's heart, started by the tap rather than the state.
        val favoriteScale = remember { Animatable(1f) }
        val favoriteBurst = remember { Animatable(1f) }
        IconToggleButton(
            checked = isFavorite,
            onCheckedChange = {
                scope.bounce(favoriteScale, motionScheme)
                if (!isFavorite) scope.launch {
                    favoriteBurst.snapTo(0f)
                    favoriteBurst.animateTo(1f, motionScheme.slowSpatialSpec())
                }
                onToggleFavorite()
            },
            shapes = IconButtonDefaults.toggleableShapes(),
            colors = IconButtonDefaults.iconToggleButtonColors(
                contentColor = Color.White,
                checkedContentColor = Color.White
            )
        ) {
            FavoriteIcon(
                isFavorite = isFavorite,
                scale = { favoriteScale.value },
                burst = { favoriteBurst.value },
                contentDescription = stringResource(R.string.cd_favorite)
            )
        }

        IconButton(
            onClick = onShowInfo,
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.cd_asset_info)
            )
        }
    }
}

/** The current photo's decision, and the only way to change it here; tapping the selected one clears it. */
@Composable
internal fun DecisionButtons(
    decision: DuplicateDecision,
    onDecisionChange: (DuplicateDecision) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple(DuplicateDecision.DELETE, Icons.Default.Delete, R.string.swipe_delete_upper),
            Triple(DuplicateDecision.KEEP, Icons.Default.Check, R.string.swipe_keep_upper)
        ).forEachIndexed { index, (option, icon, label) ->
            val checked = decision == option
            DecisionToggleButton(
                checked = checked,
                onClick = { onDecisionChange(if (checked) DuplicateDecision.NONE else option) },
                index = index,
                icon = icon,
                iconAtEnd = option == DuplicateDecision.DELETE,
                label = stringResource(label),
                checkedColor = decisionColor(option),
                contentColor = Color.White
            )
        }
    }
}

/** Previous group, position, next group: containerless, like the top bar. */
@Composable
internal fun GroupNavigator(
    label: String,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val motionScheme = MaterialTheme.motionScheme
    // White on the black backdrop, dimmed like the swipe fullscreen's undo when there's nowhere to go.
    val buttonColors = IconButtonDefaults.iconButtonColors(
        contentColor = Color.White,
        disabledContentColor = Color.White.copy(alpha = 0.38f)
    )
    val previousScale = remember { Animatable(1f) }
    val nextScale = remember { Animatable(1f) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // A size up from the top bar's buttons: without containers the row needs the extra weight.
        IconButton(
            onClick = {
                if (hasPrevious) {
                    scope.bounce(previousScale, motionScheme)
                    onPrevious()
                }
            },
            enabled = hasPrevious,
            shapes = IconButtonDefaults.shapes(),
            colors = buttonColors,
            modifier = Modifier.size(IconButtonDefaults.mediumContainerSize())
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                contentDescription = stringResource(R.string.cd_previous_group),
                modifier = Modifier
                    .size(IconButtonDefaults.mediumIconSize)
                    .graphicsLayer {
                        scaleX = previousScale.value
                        scaleY = previousScale.value
                    }
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 160.dp).padding(horizontal = 8.dp)
        )

        IconButton(
            onClick = {
                if (hasNext) {
                    scope.bounce(nextScale, motionScheme)
                    onNext()
                }
            },
            enabled = hasNext,
            shapes = IconButtonDefaults.shapes(),
            colors = buttonColors,
            modifier = Modifier.size(IconButtonDefaults.mediumContainerSize())
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = stringResource(R.string.cd_next_group),
                modifier = Modifier
                    .size(IconButtonDefaults.mediumIconSize)
                    .graphicsLayer {
                        scaleX = nextScale.value
                        scaleY = nextScale.value
                    }
            )
        }
    }
}

/** One half of the delete/keep pair: just the label at rest, the icon slides out on its outer side once it's selected. */
@Composable
private fun DecisionToggleButton(
    checked: Boolean,
    onClick: () -> Unit,
    index: Int,
    icon: ImageVector,
    iconAtEnd: Boolean,
    label: String,
    checkedColor: Color,
    contentColor: Color
) {
    val motionScheme = MaterialTheme.motionScheme
    ToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        shapes = ToggleButtonShapes(
            shape = connectedTileShape(index, 2, outer = ControlTileHeight / 2),
            pressedShape = RoundedCornerShape(10.dp),
            checkedShape = CircleShape
        ),
        // Neutral at rest, like the white chrome around it; only a decision brings in its green or red.
        colors = ToggleButtonDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.14f),
            contentColor = contentColor,
            checkedContainerColor = checkedColor,
            checkedContentColor = Color.White
        ),
        modifier = Modifier.height(ControlTileHeight),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        val slidingIcon: @Composable () -> Unit = {
            // Grows outward from the label's side.
            val anchor = if (iconAtEnd) Alignment.Start else Alignment.End
            AnimatedVisibility(
                visible = checked,
                enter = expandHorizontally(motionScheme.defaultSpatialSpec(), expandFrom = anchor) +
                    fadeIn(motionScheme.defaultEffectsSpec()),
                exit = shrinkHorizontally(motionScheme.defaultSpatialSpec(), shrinkTowards = anchor) +
                    fadeOut(motionScheme.fastEffectsSpec())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val spacer = @Composable { Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(ControlTileHeight))) }
                    if (iconAtEnd) spacer()
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.iconSizeFor(ControlTileHeight))
                    )
                    if (!iconAtEnd) spacer()
                }
            }
        }
        if (!iconAtEnd) slidingIcon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        if (iconAtEnd) slidingIcon()
    }
}
