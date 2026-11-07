package com.markvoronin.immichswipe.feature.settings.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.ButtonStackOrientation
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.ImmichOpenMode
import com.markvoronin.immichswipe.feature.settings.SettingsUiState
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel

@Composable
fun ActionButtonsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        onBack()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalFadingEdges(scrollState, length = 32.dp)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StackOrientationPickerCard(
                orientation = uiState.buttonStackOrientation,
                onOrientationSelected = { viewModel.setButtonStackOrientation(it) }
            )

            IconPositionPickerCard(
                title = stringResource(R.string.settings_fullscreen_pos_label),
                icon = Icons.Default.Fullscreen,
                selectedPosition = uiState.fullscreenButtonPosition,
                onPositionSelected = { viewModel.setFullscreenButtonPosition(it) },
                showIcon = uiState.showFullscreenButton,
                onShowIconChange = { viewModel.setShowFullscreenButton(it) }
            )

            IconPositionPickerCard(
                title = stringResource(R.string.settings_rotation_pos_label),
                icon = Icons.AutoMirrored.Filled.RotateRight,
                selectedPosition = uiState.rotationButtonPosition,
                onPositionSelected = { viewModel.setRotationButtonPosition(it) },
                showIcon = uiState.showRotationButton,
                onShowIconChange = { viewModel.setShowRotationButton(it) }
            )

            ImmichCardAction(
                title = stringResource(R.string.settings_card_immich),
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                selectedPosition = uiState.immichButtonPosition,
                onPositionSelected = { viewModel.setImmichButtonPosition(it) },
                showIcon = uiState.showImmichButton,
                onShowIconChange = { viewModel.setShowImmichButton(it) },
                openMode = uiState.immichOpenMode,
                onOpenModeSelected = { viewModel.setImmichOpenMode(it) }
            )

            IconPositionPickerCard(
                title = stringResource(R.string.settings_mute_pos_label),
                icon = Icons.AutoMirrored.Filled.VolumeOff,
                selectedPosition = uiState.muteButtonPosition,
                onPositionSelected = { viewModel.setMuteButtonPosition(it) },
                showIcon = uiState.showMuteButton,
                onShowIconChange = { viewModel.setShowMuteButton(it) }
            )

            IconPositionPickerCard(
                title = stringResource(R.string.settings_download_pos_label),
                icon = Icons.Default.FileDownload,
                selectedPosition = uiState.downloadButtonPosition,
                onPositionSelected = { viewModel.setDownloadButtonPosition(it) },
                showIcon = uiState.showDownloadButton,
                onShowIconChange = { viewModel.setShowDownloadButton(it) }
            )

            IconPositionPickerCard(
                title = stringResource(R.string.settings_share_pos_label),
                icon = Icons.Default.Share,
                selectedPosition = uiState.shareButtonPosition,
                onPositionSelected = { viewModel.setShareButtonPosition(it) },
                showIcon = uiState.showShareButton,
                onShowIconChange = { viewModel.setShowShareButton(it) }
            )

            IconPositionPickerCard(
                title = stringResource(R.string.settings_full_size_pos_label),
                icon = Icons.Default.Hd,
                selectedPosition = uiState.fullSizeButtonPosition,
                onPositionSelected = { viewModel.setFullSizeButtonPosition(it) },
                showIcon = uiState.showFullSizeButton,
                onShowIconChange = { viewModel.setShowFullSizeButton(it) },
                extraContent = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.settings_keep_full_resolution),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        SettingsSwitch(
                            checked = uiState.keepFullResolution,
                            onCheckedChange = { viewModel.setKeepFullResolution(it) },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
            )
        }

        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

@Composable
fun IconPositionPickerCard(
    title: String,
    icon: ImageVector,
    selectedPosition: IconPosition,
    onPositionSelected: (IconPosition) -> Unit,
    showIcon: Boolean,
    onShowIconChange: (Boolean) -> Unit,
    isBeta: Boolean = false,
    extraContent: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = animatedCookieShape(showIcon, MaterialTheme.motionScheme.fastSpatialSpec()),
                        color = if (showIcon) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (showIcon) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        if (isBeta) {
                            BetaBadge()
                            Spacer(Modifier.height(2.dp))
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showIcon) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SettingsSwitch(
                    checked = showIcon,
                    onCheckedChange = onShowIconChange,
                    modifier = Modifier.scale(0.8f)
                )
            }

            AnimatedVisibility(
                visible = showIcon,
                enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()) + shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec())
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            CornerButton(
                                position = IconPosition.TOP_LEFT,
                                selected = selectedPosition == IconPosition.TOP_LEFT,
                                onClick = { onPositionSelected(IconPosition.TOP_LEFT) }
                            )
                            CornerButton(
                                position = IconPosition.TOP_RIGHT,
                                selected = selectedPosition == IconPosition.TOP_RIGHT,
                                onClick = { onPositionSelected(IconPosition.TOP_RIGHT) }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            CornerButton(
                                position = IconPosition.BOTTOM_LEFT,
                                selected = selectedPosition == IconPosition.BOTTOM_LEFT,
                                onClick = { onPositionSelected(IconPosition.BOTTOM_LEFT) }
                            )
                            CornerButton(
                                position = IconPosition.BOTTOM_RIGHT,
                                selected = selectedPosition == IconPosition.BOTTOM_RIGHT,
                                onClick = { onPositionSelected(IconPosition.BOTTOM_RIGHT) }
                            )
                        }
                    }

                    if (extraContent != null) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(8.dp))
                        extraContent()
                    }
                }
            }
        }
    }
}

private val IconPosition.labelRes: Int
    get() = when (this) {
        IconPosition.TOP_LEFT -> R.string.settings_pos_top_left
        IconPosition.TOP_RIGHT -> R.string.settings_pos_top_right
        IconPosition.BOTTOM_LEFT -> R.string.settings_pos_bottom_left
        IconPosition.BOTTOM_RIGHT -> R.string.settings_pos_bottom_right
    }

@Composable
fun CornerButton(
    position: IconPosition,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outer = 24.dp
    val inner = 6.dp
    val top = position == IconPosition.TOP_LEFT || position == IconPosition.TOP_RIGHT
    val left = position == IconPosition.TOP_LEFT || position == IconPosition.BOTTOM_LEFT
    fun cornerShape(inner: Dp) = RoundedCornerShape(
        topStart = if (top && left) outer else inner,
        topEnd = if (top && !left) outer else inner,
        bottomStart = if (!top && left) outer else inner,
        bottomEnd = if (!top && !left) outer else inner
    )
    SettingsToggleButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier.width(125.dp),
        tileShape = cornerShape(inner),
        checkedTileShape = cornerShape(14.dp)
    ) {
        val edge = if (left) Alignment.Start else Alignment.End
        val arrow: @Composable () -> Unit = {
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()) + expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), expandFrom = edge),
                exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()) + shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), shrinkTowards = edge)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!left) Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = when (position) {
                            IconPosition.TOP_LEFT -> Icons.Default.NorthWest
                            IconPosition.TOP_RIGHT -> Icons.Default.NorthEast
                            IconPosition.BOTTOM_LEFT -> Icons.Default.SouthWest
                            IconPosition.BOTTOM_RIGHT -> Icons.Default.SouthEast
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    if (left) Spacer(Modifier.width(6.dp))
                }
            }
        }
        if (left) arrow()
        Text(
            text = stringResource(position.labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        if (!left) arrow()
    }
}

@Composable
fun ImmichCardAction(
    title: String,
    icon: ImageVector,
    selectedPosition: IconPosition,
    onPositionSelected: (IconPosition) -> Unit,
    showIcon: Boolean,
    onShowIconChange: (Boolean) -> Unit,
    openMode: ImmichOpenMode,
    onOpenModeSelected: (ImmichOpenMode) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = animatedCookieShape(showIcon, MaterialTheme.motionScheme.fastSpatialSpec()),
                        color = if (showIcon) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (showIcon) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (showIcon) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SettingsSwitch(
                    checked = showIcon,
                    onCheckedChange = onShowIconChange,
                    modifier = Modifier.scale(0.8f)
                )
            }

            AnimatedVisibility(
                visible = showIcon,
                enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()) + shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec())
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.settings_immich_pos_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            CornerButton(
                                position = IconPosition.TOP_LEFT,
                                selected = selectedPosition == IconPosition.TOP_LEFT,
                                onClick = { onPositionSelected(IconPosition.TOP_LEFT) }
                            )
                            CornerButton(
                                position = IconPosition.TOP_RIGHT,
                                selected = selectedPosition == IconPosition.TOP_RIGHT,
                                onClick = { onPositionSelected(IconPosition.TOP_RIGHT) }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            CornerButton(
                                position = IconPosition.BOTTOM_LEFT,
                                selected = selectedPosition == IconPosition.BOTTOM_LEFT,
                                onClick = { onPositionSelected(IconPosition.BOTTOM_LEFT) }
                            )
                            CornerButton(
                                position = IconPosition.BOTTOM_RIGHT,
                                selected = selectedPosition == IconPosition.BOTTOM_RIGHT,
                                onClick = { onPositionSelected(IconPosition.BOTTOM_RIGHT) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.settings_immich_open_mode_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
                    ) {
                        OpenModeButton(
                            text = stringResource(R.string.settings_immich_open_mode_app),
                            icon = Icons.Default.PhoneAndroid,
                            selected = openMode == ImmichOpenMode.APP,
                            onClick = { onOpenModeSelected(ImmichOpenMode.APP) },
                            index = 0, count = 2
                        )
                        OpenModeButton(
                            text = stringResource(R.string.settings_immich_open_mode_web),
                            icon = Icons.Default.Language,
                            selected = openMode == ImmichOpenMode.WEB,
                            onClick = { onOpenModeSelected(ImmichOpenMode.WEB) },
                            index = 1, count = 2
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                if (openMode == ImmichOpenMode.APP) R.string.settings_immich_long_press_web
                                else R.string.settings_immich_long_press_app
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OpenModeButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1
) {
    SettingsToggleButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier.width(125.dp),
        index = index,
        count = count
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun BetaBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Text(
            text = stringResource(R.string.common_beta),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StackOrientationPickerCard(
    orientation: ButtonStackOrientation,
    onOrientationSelected: (ButtonStackOrientation) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_button_stacking_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_button_stacking_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
            ) {
                StackOrientationButton(
                    text = stringResource(R.string.settings_button_stacking_horizontal),
                    selected = orientation == ButtonStackOrientation.HORIZONTAL,
                    isHorizontal = true,
                    onClick = { onOrientationSelected(ButtonStackOrientation.HORIZONTAL) },
                    modifier = Modifier.weight(1f),
                    index = 0, count = 2
                )
                StackOrientationButton(
                    text = stringResource(R.string.settings_button_stacking_vertical),
                    selected = orientation == ButtonStackOrientation.VERTICAL,
                    isHorizontal = false,
                    onClick = { onOrientationSelected(ButtonStackOrientation.VERTICAL) },
                    modifier = Modifier.weight(1f),
                    index = 1, count = 2
                )
            }
        }
    }
}

@Composable
fun HorizontalCirclesIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(18.dp)
) {
    Canvas(modifier = modifier) {
        val radius = size.height * 0.16f
        val cy = size.height / 2f
        val cx1 = size.width * 0.22f
        val cx2 = size.width * 0.78f
        drawCircle(color = tint, radius = radius, center = Offset(cx1, cy))
        drawCircle(color = tint, radius = radius, center = Offset(cx2, cy))
    }
}

@Composable
fun VerticalCirclesIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(18.dp)
) {
    Canvas(modifier = modifier) {
        val radius = size.width * 0.16f
        val cx = size.width / 2f
        val cy1 = size.height * 0.22f
        val cy2 = size.height * 0.78f
        drawCircle(color = tint, radius = radius, center = Offset(cx, cy1))
        drawCircle(color = tint, radius = radius, center = Offset(cx, cy2))
    }
}

@Composable
fun StackOrientationButton(
    text: String,
    selected: Boolean,
    isHorizontal: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1
) {
    val tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline

    SettingsToggleButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        index = index,
        count = count
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()) + expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), expandFrom = Alignment.Start),
            exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()) + shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), shrinkTowards = Alignment.Start)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isHorizontal) {
                    HorizontalCirclesIcon(tint = tint)
                } else {
                    VerticalCirclesIcon(tint = tint)
                }
                Spacer(Modifier.width(8.dp))
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
