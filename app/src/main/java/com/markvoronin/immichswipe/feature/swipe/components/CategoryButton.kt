package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SortCategory
import com.markvoronin.immichswipe.feature.settings.components.JiggleRow

/**
 * Date / Size / Type selector as a connected button group, like the swipe action bar: separate tonal tiles with a
 * gap between them, fully rounded only at the outer ends. The tapped tile grows and its neighbours give way.
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
            SortCategory.TIME to R.string.sort_category_time,
            SortCategory.SIZE to R.string.sort_category_size,
            SortCategory.TYPE to R.string.sort_category_type
        )
    }

    JiggleRow(
        count = categories.size,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        grow = 0.14f
    ) { jiggle ->
        val pressedShape = RoundedCornerShape(10.dp)
        categories.forEachIndexed { index, (category, stringRes) ->
            val selected = category == selectedCategory
            val shape = connectedTileShape(index, categories.size, outer = 24.dp)
            ToggleButton(
                checked = selected,
                onCheckedChange = { jiggle.tap(index); onCategorySelected(category) },
                shapes = ToggleButtonShapes(shape = shape, pressedShape = pressedShape, checkedShape = shape),
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.weight(jiggle.factor(index)).height(48.dp)
            ) {
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
