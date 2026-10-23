package com.markvoronin.immichswipe.feature.swipe

import com.markvoronin.immichswipe.feature.swipe.utils.formatMediaTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeFormatUtilsTest {
    @Test
    fun `media time is minutes and seconds below an hour`() {
        assertEquals("0:00", formatMediaTime(0))
        assertEquals("0:59", formatMediaTime(59_999))
        assertEquals("1:01", formatMediaTime(61_000))
        assertEquals("10:00", formatMediaTime(600_000))
    }

    @Test
    fun `media time adds hours from one hour on`() {
        assertEquals("1:00:00", formatMediaTime(3_600_000))
        assertEquals("1:01:01", formatMediaTime(3_661_000))
    }
}
