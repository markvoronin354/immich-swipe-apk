package com.markvoronin.immichswipe.domain.model

import com.google.gson.Gson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumTest {

    private fun album(ownerId: String?, members: List<AlbumUser>?) = Album(
        id = "a", albumName = "Album", assetCount = 1, albumThumbnailAssetId = null,
        ownerId = ownerId, albumUsers = members
    )

    private fun member(id: String, role: String) = AlbumUser(AlbumMember(id), role)

    @Test
    fun `the owner can always add`() {
        assertTrue(album("me", emptyList()).canAddAssets("me"))
    }

    @Test
    fun `an editor can add and a viewer cannot`() {
        val shared = album("owner", listOf(member("me", "editor"), member("you", "viewer")))

        assertTrue(shared.canAddAssets("me"))
        assertFalse(shared.canAddAssets("you"))
    }

    @Test
    fun `someone who is not a member cannot add`() {
        assertFalse(album("owner", listOf(member("you", "editor"))).canAddAssets("me"))
    }

    @Test
    fun `a server that reports no owner or members hides nothing`() {
        assertTrue(album(null, null).canAddAssets("me"))
        assertTrue(album("owner", null).canAddAssets("me"))
    }

    @Test
    fun `an album parsed without the new fields hides nothing`() {
        // Gson skips the Kotlin defaults, so absent fields arrive as null; this is what an older server sends.
        val parsed = Gson().fromJson("""{"id":"a","albumName":"Old","assetCount":1}""", Album::class.java)

        assertTrue(parsed.canAddAssets("me"))
    }

    @Test
    fun `an album parsed from a current server carries its members`() {
        val json = """
            {"id":"a","albumName":"Shared","assetCount":1,"ownerId":"owner",
             "albumUsers":[{"user":{"id":"me","name":"Me"},"role":"viewer"}]}
        """.trimIndent()

        val parsed = Gson().fromJson(json, Album::class.java)

        assertFalse(parsed.canAddAssets("me"))
        assertTrue(parsed.canAddAssets("owner"))
    }
}
