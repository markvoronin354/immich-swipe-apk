package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.IconPosition.BOTTOM_LEFT
import com.markvoronin.immichswipe.core.IconPosition.TOP_LEFT
import com.markvoronin.immichswipe.core.IconPosition.TOP_RIGHT
import org.junit.Assert.assertEquals
import org.junit.Test

class FittingButtonSizeTest {
    private fun size(counts: Map<IconPosition, Int>, horizontal: Boolean, width: Float, height: Float) =
        fittingButtonSize(counts, horizontal, width.dp, height.dp).value

    @Test
    fun `buttons that fit keep their full size`() {
        assertEquals(48f, size(mapOf(TOP_LEFT to 3, TOP_RIGHT to 2), horizontal = true, width = 344f, height = 500f), 0.01f)
        assertEquals(48f, size(emptyMap(), horizontal = true, width = 344f, height = 500f), 0.01f)
    }

    @Test
    fun `seven buttons in one row just fit a 352dp card`() {
        assertEquals(48f, size(mapOf(TOP_LEFT to 7), horizontal = true, width = 352f, height = 500f), 0.01f)
    }

    @Test
    fun `a row too long for the card shrinks its buttons`() {
        // 320 - 16 inset = 304 for seven buttons.
        assertEquals(304f / 7, size(mapOf(TOP_LEFT to 7), horizontal = true, width = 320f, height = 500f), 0.01f)
    }

    @Test
    fun `two corners along one edge share its room`() {
        // 320 - 2 x 16 inset = 288 for seven buttons.
        assertEquals(288f / 7, size(mapOf(TOP_LEFT to 4, TOP_RIGHT to 3), horizontal = true, width = 320f, height = 500f), 0.01f)
    }

    @Test
    fun `buttons never shrink below 40dp`() {
        assertEquals(40f, size(mapOf(TOP_LEFT to 7), horizontal = true, width = 200f, height = 500f), 0.01f)
    }

    @Test
    fun `a stacked pair of columns shares the card's height`() {
        // 320 - 2 x 16 inset = 288 for seven buttons.
        assertEquals(288f / 7, size(mapOf(TOP_LEFT to 4, BOTTOM_LEFT to 3), horizontal = false, width = 344f, height = 320f), 0.01f)
    }

    @Test
    fun `a row only needs the card's width and a column only its height`() {
        assertEquals(48f, size(mapOf(TOP_LEFT to 7), horizontal = true, width = 500f, height = 100f), 0.01f)
        assertEquals(48f, size(mapOf(TOP_LEFT to 7), horizontal = false, width = 100f, height = 500f), 0.01f)
    }
}
