package com.markvoronin.immichswipe.feature.duplicates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceTilesTest {
    private fun assertTilesCoverEachPixelOnce(width: Int, height: Int) {
        val tiles = tilesFor(width, height)
        for (x in listOf(0, 1, width / 3, width / 2, width - 2, width - 1)) {
            for (y in listOf(0, 1, height / 3, height / 2, height - 2, height - 1)) {
                val owners = tiles.filter { x >= it.cellLeft && x < it.cellRight && y >= it.cellTop && y < it.cellBottom }
                assertEquals("pixel $x,$y in ${width}x$height", 1, owners.size)
            }
        }
    }

    @Test
    fun `every pixel belongs to the cell of exactly one tile`() {
        assertTilesCoverEachPixelOnce(4000, 2250)
        assertTilesCoverEachPixelOnce(2048, 1536)
        assertTilesCoverEachPixelOnce(385, 1000)
    }

    @Test
    fun `a tile always contains its own cell and stays inside the image`() {
        tilesFor(4000, 2250).forEach {
            assertTrue(it.x >= 0 && it.y >= 0 && it.x + it.width <= 4000 && it.y + it.height <= 2250)
            assertTrue(it.x <= it.cellLeft && it.x + it.width >= it.cellRight)
            assertTrue(it.y <= it.cellTop && it.y + it.height >= it.cellBottom)
        }
    }

    @Test
    fun `an image smaller than a tile gets a single tile`() {
        val tiles = tilesFor(300, 200)
        assertEquals(1, tiles.size)
        assertEquals(300, tiles[0].width)
        assertEquals(200, tiles[0].height)
    }
}
