package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumDropStateTest {
    private fun state() = AlbumDropState().apply {
        deckOrigin = Offset(0f, 100f)
        tileRects["a"] = Rect(Offset(0f, 1000f), androidx.compose.ui.geometry.Size(100f, 100f))
        tileRects["b"] = Rect(Offset(120f, 1000f), androidx.compose.ui.geometry.Size(100f, 100f))
    }

    @Test
    fun `a finger above the strip is over nothing`() {
        val state = state()
        state.move(Offset(160f, 300f))

        assertNull(state.hoveredId)
    }

    @Test
    fun `a finger in the strip is over the nearest tile`() {
        val state = state()
        state.move(Offset(180f, 950f))

        assertEquals("b", state.hoveredId)
    }

    @Test
    fun `a finger just above the strip already picks a tile`() {
        val state = state()
        state.move(Offset(30f, 850f))

        assertEquals("a", state.hoveredId)
    }

    private val row = Rect(Offset(0f, 1000f), androidx.compose.ui.geometry.Size(400f, 120f))

    @Test
    fun `the strip scrolls towards an edge the finger nears, faster the closer`() {
        assertEquals(-1f, autoScrollDirection(Offset(-20f, 1050f), row, 64f), 0.001f)
        assertEquals(-0.5f, autoScrollDirection(Offset(32f, 1050f), row, 64f), 0.001f)
        assertEquals(0.5f, autoScrollDirection(Offset(368f, 1050f), row, 64f), 0.001f)
        assertEquals(1f, autoScrollDirection(Offset(400f, 1050f), row, 64f), 0.001f)
    }

    @Test
    fun `the strip stays put mid-row, over More and while the finger is far above`() {
        assertEquals(0f, autoScrollDirection(Offset(200f, 1050f), row, 64f), 0f)
        assertEquals(0f, autoScrollDirection(Offset(450f, 1050f), row, 64f), 0f)
        assertEquals(0f, autoScrollDirection(Offset(10f, 300f), row, 64f), 0f)
    }

    @Test
    fun `ending the drag clears the hover`() {
        val state = state()
        state.move(Offset(30f, 950f))
        state.end()

        assertFalse(state.isActive)
        assertNull(state.hoveredId)
    }
}
