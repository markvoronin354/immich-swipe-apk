package com.markvoronin.immichswipe.navigation

import android.content.Intent
import com.markvoronin.immichswipe.domain.model.Album
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkHandlerTest {
    private fun parse(uri: String) = DeepLinkHandler.parseUri(uri)

    @Test
    fun `swipe and album links open the album from the path`() {
        assertEquals(NavKey.Swipe("abc"), parse("immichswipe://swipe/abc"))
        assertEquals(NavKey.Swipe("abc"), parse("immichswipe://album/abc"))
    }

    @Test
    fun `the album id can also come from a query parameter`() {
        assertEquals(NavKey.Swipe("abc"), parse("immichswipe://swipe?id=abc"))
    }

    @Test
    fun `a swipe link without an id opens all photos`() {
        assertEquals(NavKey.Swipe(Album.VIRTUAL_ALL_ID), parse("immichswipe://swipe"))
    }

    @Test
    fun `friendly names map to the virtual albums`() {
        assertEquals(NavKey.Swipe(Album.VIRTUAL_ALL_ID), parse("immichswipe://swipe/all"))
        assertEquals(NavKey.Swipe(Album.VIRTUAL_ALL_ID), parse("immichswipe://swipe/virtual-all"))
        assertEquals(NavKey.Swipe(Album.VIRTUAL_ORPHANS_ID), parse("immichswipe://swipe/orphans"))
        assertEquals(NavKey.Swipe(Album.VIRTUAL_DUPLICATES_ID), parse("immichswipe://album/duplicates"))
    }

    @Test
    fun `top level destinations are recognised`() {
        assertEquals(NavKey.Duplicates, parse("immichswipe://duplicates"))
        assertEquals(NavKey.Settings, parse("immichswipe://settings"))
    }

    @Test
    fun `home links can point further into the app`() {
        assertEquals(NavKey.Home, parse("immichswipe://home"))
        assertEquals(NavKey.Home, parse("immichswipe://home/unknown"))
        assertEquals(NavKey.Swipe("abc"), parse("immichswipe://home/swipe/abc"))
        assertEquals(NavKey.Swipe(Album.VIRTUAL_ALL_ID), parse("immichswipe://home/album"))
        assertEquals(NavKey.Duplicates, parse("immichswipe://home/duplicates"))
        assertEquals(NavKey.Settings, parse("immichswipe://home/settings"))
    }

    @Test
    fun `web links are matched on the host like app links`() {
        assertEquals(NavKey.Swipe("abc"), parse("https://swipe/abc"))
        assertEquals(NavKey.Settings, parse("http://settings"))
    }

    @Test
    fun `unknown schemes and hosts are ignored`() {
        assertNull(parse("ftp://swipe/abc"))
        assertNull(parse("immichswipe://nowhere"))
        assertNull(parse("immichswipe:///swipe/abc"))
    }

    @Test
    fun `intents without data are ignored`() {
        assertNull(DeepLinkHandler.parseIntent(null))
        val intent = mockk<Intent>()
        every { intent.dataString } returns null
        assertNull(DeepLinkHandler.parseIntent(intent))
    }

    @Test
    fun `intents carry their link through`() {
        val intent = mockk<Intent>()
        every { intent.dataString } returns "immichswipe://duplicates"
        assertEquals(NavKey.Duplicates, DeepLinkHandler.parseIntent(intent))
    }
}
