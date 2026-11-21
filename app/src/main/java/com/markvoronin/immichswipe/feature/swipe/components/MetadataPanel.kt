package com.markvoronin.immichswipe.feature.swipe.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.swipe.utils.formatSize
import com.markvoronin.immichswipe.feature.swipe.utils.samsungModelName
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

@Composable
fun MetadataPanel(
    asset: Asset,
    albumNames: List<String>?,
    showAlbums: Boolean = true,
    backdrop: Backdrop,
    onClose: () -> Unit,
    onSaveDescription: suspend (String) -> Boolean = { false },
    onDrag: (Float) -> Unit = {},
    onDragEnd: (Float) -> Unit = {},
    panelProgress: Float = 0f,
    maxHeightPx: Float = 0f
) {
    val scrollState = rememberScrollState()
    var isEditingDescription by remember(asset.id) { mutableStateOf(false) }

    LaunchedEffect(panelProgress) {
        if (panelProgress == 0f) {
            scrollState.scrollTo(0)
        }
    }

    // Connection to pass downward drags to the parent when at the top of the scroll
    val nestedScrollConnection = remember(scrollState, panelProgress, maxHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If dragging DOWN (available.y > 0) and at top of scroll, consume to move panel down
                // If dragging UP (available.y < 0) and at top of scroll when panel is not fully expanded, consume to move panel up
                if (available.y > 0 && scrollState.value == 0) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                } else if (available.y < 0 && scrollState.value == 0 && panelProgress < 1f) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Handle cases where scroll reached the top during the gesture
                if (available.y > 0 && scrollState.value == 0) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                }
                // Fully open and the content has nothing left to scroll: keep pulling to stretch the panel.
                // Only for finger input; consuming fling leftovers would keep the fling (and the spring-back) running.
                if (available.y < 0 && panelProgress >= 1f && source == NestedScrollSource.UserInput) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val totalVelocityY = consumed.y + available.y
                onDragEnd(totalVelocityY)
                return super.onPostFling(consumed, available)
            }
        }
    }

    // Same corner radius as the swipe card so the panel sits exactly on top of it.
    val panelShape = RoundedCornerShape(16.dp)
    // Glass is tinted with the theme's container colour; light theme gets dark text, dark theme light text.
    val isLightTheme = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val glassTint = MaterialTheme.colorScheme.surfaceContainer
    val glassContent = if (isLightTheme) Color.Black else Color.White
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(nestedScrollConnection)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { panelShape },
                // The default rim highlight and drop shadow trace the edge and read as a seam against the card.
                highlight = null,
                shadow = null,
                effects = {
                    colorControls(
                        brightness = 0f,   // default 0f. Positive brightens, negative darkens.
                        contrast = 1f,     // default 1f. Above 1 punchier, below 1 flatter.
                        saturation = 1f  // default 1f. 1.5f is what vibrancy() does. 0f is greyscale.
                    )
                    blur(20.dp.toPx())
                },
                onDrawSurface = { drawRect(glassTint.copy(alpha = .5f)) }
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = glassContent),
        shape = panelShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.size(40.dp, 4.dp).clip(CircleShape).background(glassContent.copy(alpha = 0.55f)).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val titleRes = if (asset.isVideo) R.string.swipe_metadata_title_video else R.string.swipe_metadata_title
                Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                // Offset cancels the button's internal padding so the glyph's right edge meets the content edge.
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp).offset(x = 8.dp)) {
                    Icon(Icons.Default.Close, stringResource(R.string.common_close), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            DescriptionRow(
                description = asset.exifInfo?.description?.takeIf { it.isNotBlank() },
                onClick = { isEditingDescription = true }
            )
            AssetMetadataRows(asset = asset, albumNames = albumNames, showAlbums = showAlbums)
        }
    }

    if (isEditingDescription) {
        DescriptionDialog(
            initial = asset.exifInfo?.description.orEmpty(),
            onSave = onSaveDescription,
            onDismiss = { isEditingDescription = false }
        )
    }
}

/** The detail rows of [MetadataPanel], shared with the duplicates info dialog, which also lists the file name. */
@Composable
internal fun AssetMetadataRows(asset: Asset, albumNames: List<String>?, showFileName: Boolean = false, showAlbums: Boolean = true) {
    if (showFileName) {
        MetadataRow(
            Icons.Outlined.Description,
            stringResource(R.string.swipe_metadata_file),
            asset.originalFileName ?: stringResource(R.string.swipe_metadata_unavailable),
            showDivider = false
        )
    }
    val date = runCatching { LocalDate.parse(asset.fileCreatedAt.substringBefore("T")) }.getOrNull()
        ?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        ?: asset.fileCreatedAt.substringBefore("T")
    MetadataRow(Icons.Outlined.CalendarMonth, stringResource(R.string.swipe_metadata_date), date, showDivider = showFileName)
    val extension = (asset.fileExtension ?: asset.originalFileName?.substringAfterLast('.', ""))
        ?.removePrefix(".")?.takeIf { it.isNotEmpty() }
    val typeLabel = asset.type.lowercase().replaceFirstChar { it.uppercase() }
    val formatLabel = if (extension != null) "$typeLabel (.${extension.uppercase()})" else typeLabel
    MetadataRow(Icons.Outlined.Info, stringResource(R.string.swipe_metadata_format), formatLabel)
    asset.exifInfo?.let { exif ->
        val sizeLabel = exif.fileSizeInBytes?.let { formatSize(it) } ?: stringResource(R.string.swipe_metadata_unavailable)
        MetadataRow(Icons.Outlined.Storage, stringResource(R.string.swipe_metadata_size), sizeLabel)
        MetadataRow(Icons.Outlined.AspectRatio, stringResource(R.string.swipe_metadata_resolution), "${exif.imageWidth ?: "?"} × ${exif.imageHeight ?: "?"}")
        val camera = cameraLabel(exif.make, exif.model)
        if (camera.isNotEmpty()) {
            MetadataRow(Icons.Outlined.PhotoCamera, stringResource(R.string.swipe_metadata_camera), camera)
        }
    } ?: run {
        MetadataRow(Icons.Outlined.Storage, stringResource(R.string.swipe_metadata_size), stringResource(R.string.swipe_loading))
        MetadataRow(Icons.Outlined.AspectRatio, stringResource(R.string.swipe_metadata_resolution), stringResource(R.string.swipe_loading))
    }
    if (showAlbums) {
        val albumsLabel = when {
            albumNames == null -> stringResource(R.string.swipe_loading)
            albumNames.isEmpty() -> stringResource(R.string.swipe_metadata_no_albums)
            else -> albumNames.joinToString(", ")
        }
        MetadataRow(Icons.Outlined.Collections, stringResource(R.string.swipe_metadata_albums), albumsLabel)
    }
}

private val CompanyWords = Regex("""(?<!\w)(Co\.?,?\s*Ltd\.?|Corporation|Corp\.?|Company|Inc\.?|Ltd\.?|Imaging)(?!\w)""", RegexOption.IGNORE_CASE)

/** Drops corporate suffixes from a camera make, e.g. "NIKON CORPORATION" -> "NIKON". */
private fun stripCompanyWords(make: String): String =
    make.replace(CompanyWords, "").replace(Regex("""\s+"""), " ").trim(' ', ',')

/** Joins make and model, dropping the make from the model when it is repeated, e.g. "SAMSUNG SAMSUNG-SM-G900A" -> "SAMSUNG SM-G900A". */
private fun cameraLabel(make: String?, model: String?): String {
    val cleanMake = make?.let(::stripCompanyWords)?.takeIf { it.isNotBlank() }
    val cleanModel = model?.trim()?.let { m ->
        val repeatsMake = cleanMake != null && m.startsWith(cleanMake, ignoreCase = true) &&
            m.getOrNull(cleanMake.length)?.isLetterOrDigit() != true
        if (repeatsMake) m.drop(cleanMake.length).trimStart(' ', '-', '_') else m
    }?.takeIf { it.isNotBlank() }
    // Samsung only exposes model codes; show the marketing name and keep the code for reference.
    if (cleanMake?.contains("samsung", ignoreCase = true) == true && cleanModel != null) {
        samsungModelName(cleanModel)?.let { return "$it ($cleanModel)" }
    }
    return listOfNotNull(cleanMake, cleanModel).joinToString(" ")
}

@Composable
fun MetadataRow(icon: ImageVector, label: String, value: String, showDivider: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (showDivider) {
                HorizontalDivider(thickness = 0.5.dp, color = LocalContentColor.current.copy(alpha = 0.2f))
            }
            Row(modifier = Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.7f))
                Spacer(Modifier.width(12.dp))
                Text(text = value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** The description, or a prompt to add one. The text can run to several lines, so the icon stays beside the first. */
@Composable
private fun DescriptionRow(description: String?, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick)) {
        Box(modifier = Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.Notes, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
                if (description != null) {
                    Text(text = description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 10.dp))
                } else {
                    Text(text = stringResource(R.string.swipe_metadata_add_description), style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.7f))
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = LocalContentColor.current.copy(alpha = 0.2f))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DescriptionDialog(
    initial: String,
    onSave: suspend (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    // Saving is a network call: lock the dialog while it runs so it can't be saved twice or dismissed mid-request.
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val errorMessage = stringResource(R.string.swipe_metadata_description_error)
    val trimmed = text.trim()
    val canSave = !saving && trimmed != initial.trim()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.swipe_metadata_description),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.swipe_metadata_add_description)) },
                readOnly = saving,
                minLines = 3,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saving = true
                    scope.launch {
                        if (onSave(trimmed)) {
                            onDismiss()
                        } else {
                            saving = false
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = canSave
            ) {
                Text(stringResource(R.string.swipe_metadata_description_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
