package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import com.markvoronin.immichswipe.feature.settings.components.SettingsToggleButton

private val CategoryButtonWidth = 90.dp

/** Expressive Date / Size / Type selector: the selected button morphs its shape and the group jiggles on tap. */
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
        horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.CenterHorizontally)
    ) { jiggle ->
        categories.forEachIndexed { index, (category, stringRes) ->
            val selected = category == selectedCategory
            SettingsToggleButton(
                selected = selected,
                onClick = { jiggle.tap(index); onCategorySelected(category) },
                modifier = Modifier.width(CategoryButtonWidth * jiggle.factor(index))
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
