package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SortCategory

/**
 * Date / Size / Type selector as a connected button group, like the swipe action bar: separate tonal tiles with a
 * gap between them, fully rounded only at the outer ends. The selected tile morphs into a pill.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategorySegmentedRow(
    selectedCategory: SortCategory,
    onCategorySelected: (SortCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember {
        listOf(
            Triple(SortCategory.TIME, R.string.sort_category_time, Icons.Default.CalendarToday),
            Triple(SortCategory.SIZE, R.string.sort_category_size, Icons.Default.Storage),
            Triple(SortCategory.TYPE, R.string.sort_category_type, Icons.Default.PermMedia)
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val pressedShape = RoundedCornerShape(10.dp)
        categories.forEachIndexed { index, (category, stringRes, icon) ->
            val selected = category == selectedCategory
            val shape = connectedTileShape(index, categories.size, outer = 24.dp)
            ToggleButton(
                checked = selected,
                onCheckedChange = { onCategorySelected(category) },
                shapes = ToggleButtonShapes(shape = shape, pressedShape = pressedShape, checkedShape = CircleShape),
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                }
                Text(
                    text = stringResource(stringRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategorySegmentedRowPreview() {
    var selected by remember { mutableStateOf(SortCategory.TIME) }
    Box(modifier = Modifier.padding(16.dp)) {
        CategorySegmentedRow(
            selectedCategory = selected,
            onCategorySelected = { selected = it }
        )
    }
}
