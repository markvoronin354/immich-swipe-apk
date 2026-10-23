package com.markvoronin.immichswipe.feature.duplicates

import com.markvoronin.immichswipe.feature.duplicates.components.formatMediaTime
import com.markvoronin.immichswipe.feature.duplicates.components.formatMiddleEllipsisFileName
import com.markvoronin.immichswipe.feature.duplicates.components.formatSizeStr
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class DuplicatesFormatUtilsTest {
    private val defaultLocale = Locale.getDefault()

    @Before
    fun useUsLocale() = Locale.setDefault(Locale.US)

    @After
    fun restoreLocale() = Locale.setDefault(defaultLocale)

    @Test
    fun `short file names are left alone`() {
        assertEquals("IMG_0001.jpg", formatMiddleEllipsisFileName("IMG_0001.jpg"))
        assertEquals("a".repeat(22), formatMiddleEllipsisFileName("a".repeat(22)))
    }

    @Test
    fun `long file names keep the start of the name and the extension`() {
        val result = formatMiddleEllipsisFileName("IMG_20240101_123456_extra_long_name.jpg")
        assertEquals("IMG_…tra_long_name.jpg", result)
        assertEquals(22, result.length)
    }

    @Test
    fun `long file names without an extension are shortened in the middle`() {
        assertEquals(
            "a_ve…without_extension",
            formatMiddleEllipsisFileName("a_very_long_file_name_without_extension")
        )
    }

    @Test
    fun `an extension that leaves no room for the base falls back to plain shortening`() {
        val result = formatMiddleEllipsisFileName("photo.verylongext", maxLength = 10)
        assertEquals("ph…longext", result)
    }

    @Test
    fun `a tiny limit just truncates`() {
        assertEquals("a", formatMiddleEllipsisFileName("abcdef", maxLength = 1))
    }

    @Test
    fun `a leading or trailing dot is not treated as an extension`() {
        assertEquals(22, formatMiddleEllipsisFileName(".hidden_config_file_name_long").length)
        assertEquals(22, formatMiddleEllipsisFileName("archive_name_that_is_really_long.").length)
    }

    @Test
    fun `sizes are shown in the largest unit that fits`() {
        assertEquals("2 KB", formatSizeStr(2 * 1024L))
        assertEquals("1023 KB", formatSizeStr(1023 * 1024L))
        assertEquals("1.00 MB", formatSizeStr(1024L * 1024))
        assertEquals("1.50 GB", formatSizeStr(1_610_612_736L))
    }

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
