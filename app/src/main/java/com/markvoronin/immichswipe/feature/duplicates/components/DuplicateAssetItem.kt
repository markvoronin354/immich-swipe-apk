package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialGreen
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialRed

private val ThumbnailOuterCorner = 16.dp
private val ThumbnailInnerCorner = 6.dp
private val ThumbnailDecidedCorner = 24.dp
private val ThumbnailFrameInset = 6.dp

/** Keep and delete use the same green and red as the header's legend. */
internal fun decisionColor(decision: DuplicateDecision): Color =
    if (decision == DuplicateDecision.DELETE) MaterialRed else MaterialGreen

@Composable
fun DuplicateAssetItem(
    asset: Asset,
    decision: DuplicateDecision,
    isBeingZoomed: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    // Reports this thumbnail's on-screen bounds (null once it leaves composition); the screen runs the pinch-to-peek gesture.
    onBoundsChanged: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
    // The ends of the cluster's connected group get the large outer corners.
    isFirst: Boolean = true,
    isLast: Boolean = true,
    baseUrl: String = "",
    isFavorite: Boolean = false,
) {
    val context = LocalContext.current
    val baseUrlClean = baseUrl.removeSuffix("/")

    val currentOnToggle by rememberUpdatedState(onToggle)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnBoundsChanged by rememberUpdatedState(onBoundsChanged)

    DisposableEffect(asset.id) {
        onDispose { currentOnBoundsChanged(null) }
    }

    val motionScheme = MaterialTheme.motionScheme
    val transition = updateTransition(decision, label = "decision")
    // A decided photo rounds off and a frame of its decision's color grows in from the edges, like a checked button.
    val decided by transition.animateFloat({ motionScheme.defaultSpatialSpec() }, label = "decided") {
        if (it == DuplicateDecision.NONE) 0f else 1f
    }
    val deleted by transition.animateFloat({ motionScheme.defaultEffectsSpec() }, label = "deleted") {
        if (it == DuplicateDecision.DELETE) 1f else 0f
    }
    // Undecided has no frame, so it keeps the color of the decision it comes from or goes to.
    val frameColor by transition.animateColor({ motionScheme.defaultEffectsSpec() }, label = "frame") { state ->
        val shown = when {
            state != DuplicateDecision.NONE -> state
            transition.targetState != DuplicateDecision.NONE -> transition.targetState
            else -> transition.currentState
        }
        decisionColor(shown)
    }
    val restingStart = if (isFirst) ThumbnailOuterCorner else ThumbnailInnerCorner
    val restingEnd = if (isLast) ThumbnailOuterCorner else ThumbnailInnerCorner
    fun cellShape(progress: Float): RoundedCornerShape {
        val start = lerp(restingStart, ThumbnailDecidedCorner, progress)
        val end = lerp(restingEnd, ThumbnailDecidedCorner, progress)
        return RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
    }
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val scrimColor = MaterialTheme.colorScheme.scrim

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.75f)
                .onGloballyPositioned { coords -> currentOnBoundsChanged(coords.boundsInWindow()) }
                .graphicsLayer {
                    alpha = if (isBeingZoomed) 0f else 1f
                    shape = cellShape(decided.coerceAtLeast(0f))
                    clip = true
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { currentOnToggle() },
                        onLongPress = { currentOnLongPress() }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val fade = deleted.coerceIn(0f, 1f)
                        colorFilter = if (fade > 0f) {
                            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1f - fade) })
                        } else null
                    }
                    .background(placeholderColor)
                    .drawWithContent {
                        drawContent()
                        val fade = deleted.coerceIn(0f, 1f)
                        if (fade > 0f) drawRect(scrimColor, alpha = 0.32f * fade)
                    }
            ) {
                val imageRequest = remember(asset.id, baseUrlClean, asset.isGif) {
                    ImageRequest.Builder(context)
                        .data(
                            if (asset.isGif) "$baseUrlClean/api/assets/${asset.id}/original"
                            else "$baseUrlClean/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview"
                        )
                        .crossfade(true)
                        .build()
                }

                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BrokenImage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            }

            // The heart, play and GIF badges sit outside the delete fade, around 24dp in, the centre of the decided
            // corner's arc, so they clear the frame and the corner.
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .size(20.dp)
                )
            }

            if (asset.type == "VIDEO") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_video),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (asset.isGif) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GIF",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // A stroke twice the frame width on the clipped outline leaves an even frame with concentric inner corners.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val progress = decided.coerceAtLeast(0f)
                        if (progress > 0.01f) {
                            drawOutline(
                                outline = cellShape(progress).createOutline(size, layoutDirection, this),
                                color = frameColor,
                                style = Stroke(width = 2 * ThumbnailFrameInset.toPx() * progress)
                            )
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val sizeStr = asset.exifInfo?.fileSizeInBytes?.let { formatSizeStr(it) } ?: stringResource(R.string.duplicates_unknown_size)
        Text(
            text = sizeStr,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/** Keep is the header's green cookie, delete its red gem; the badge pops in and swaps on each tap. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DecisionBadge(decision: DuplicateDecision, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val motionScheme = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = decision,
        modifier = modifier,
        transitionSpec = {
            (scaleIn(motionScheme.fastSpatialSpec()) + fadeIn(motionScheme.fastEffectsSpec())) togetherWith
                (scaleOut(motionScheme.fastSpatialSpec()) + fadeOut(motionScheme.fastEffectsSpec())) using
                SizeTransform(clip = false)
        },
        contentAlignment = Alignment.Center,
        label = "decisionBadge"
    ) { state ->
        if (state == DuplicateDecision.NONE) {
            Spacer(Modifier.size(size))
        } else {
            val isDelete = state == DuplicateDecision.DELETE
            Box(
                modifier = Modifier
                    .size(size)
                    .background(
                        color = decisionColor(state),
                        shape = if (isDelete) MaterialShapes.Gem.toShape() else MaterialShapes.Cookie6Sided.toShape()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDelete) Icons.Default.Delete else Icons.Default.Check,
                    contentDescription = stringResource(if (isDelete) R.string.swipe_delete else R.string.swipe_keep),
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.56f)
                )
            }
        }
    }
}
