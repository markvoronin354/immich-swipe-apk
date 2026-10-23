package com.markvoronin.immichswipe.feature.home

import com.markvoronin.immichswipe.domain.model.Album
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateTest {

    private fun album(id: String, name: String = id, count: Int = 10, description: String? = null) =
        Album(id = id, albumName = name, description = description, assetCount = count, albumThumbnailAssetId = null)

    private fun ids(albums: List<Album>) = albums.map { it.id }

    @Test
    fun `virtual albums are listed first and only when they have assets`() {
        val state = HomeUiState(
            albums = listOf(album("a1")),
            allAssetsCount = 100,
            orphansCount = 0,
            duplicatesCount = 4
        )
        assertEquals(
            listOf(Album.VIRTUAL_ALL_ID, Album.VIRTUAL_DUPLICATES_ID, "a1"),
            ids(state.filteredAlbums)
        )
        assertEquals(listOf(100, 4, 10), state.filteredAlbums.map { it.assetCount })
    }

    @Test
    fun `virtual albums take their display names and descriptions from the state`() {
        val state = HomeUiState(
            allAssetsCount = 1,
            virtualNames = mapOf(Album.VIRTUAL_ALL_ID to "Everything"),
            virtualDescriptions = mapOf(Album.VIRTUAL_ALL_ID to "All of it")
        )
        val all = state.filteredAlbums.single()
        assertEquals("Everything", all.albumName)
        assertEquals("All of it", all.description)
    }

    @Test
    fun `the SKIP album is hidden while the feature is off`() {
        val state = HomeUiState(albums = listOf(album("a1"), album("s", name = "skip")))
        assertEquals(listOf("a1"), ids(state.filteredAlbums))
    }

    @Test
    fun `search matches names and descriptions ignoring case`() {
        val albums = listOf(
            album("a1", name = "Holiday 2024"),
            album("a2", name = "Work", description = "Office HOLIDAY party"),
            album("a3", name = "Pets")
        )
        val state = HomeUiState(albums = albums, searchQuery = "holiday")
        assertEquals(listOf("a1", "a2"), ids(state.filteredAlbums))
    }

    @Test
    fun `a blank search shows everything`() {
        val state = HomeUiState(albums = listOf(album("a1"), album("a2")), searchQuery = "   ")
        assertEquals(listOf("a1", "a2"), ids(state.filteredAlbums))
    }

    @Test
    fun `search uses the renamed virtual album name`() {
        val state = HomeUiState(
            allAssetsCount = 5,
            virtualNames = mapOf(Album.VIRTUAL_ALL_ID to "Everything"),
            searchQuery = "every"
        )
        assertEquals(listOf(Album.VIRTUAL_ALL_ID), ids(state.filteredAlbums))
    }

    @Test
    fun `albums are grouped by how much of them has been treated`() {
        val state = HomeUiState(
            albums = listOf(
                album("new", count = 10),
                album("partial", count = 10),
                album("done", count = 10),
                album("over", count = 10)
            ),
            allAssetsCount = 3,
            albumTreatedCounts = mapOf("partial" to 4, "done" to 10, "over" to 25)
        )
        val groups = state.groupedAlbums

        assertEquals(listOf(Album.VIRTUAL_ALL_ID), ids(groups.getValue(AlbumStatus.VIRTUAL)))
        assertEquals(listOf("new"), ids(groups.getValue(AlbumStatus.NOT_STARTED)))
        assertEquals(listOf("partial"), ids(groups.getValue(AlbumStatus.IN_PROGRESS)))
        // More decisions than assets (assets removed since) still counts as completed.
        assertEquals(listOf("done", "over"), ids(groups.getValue(AlbumStatus.COMPLETED)))
    }

    @Test
    fun `an empty album with a treated count is completed not in progress`() {
        val state = HomeUiState(albums = listOf(album("empty", count = 0)), albumTreatedCounts = mapOf("empty" to 3))
        assertEquals(listOf("empty"), ids(state.groupedAlbums.getValue(AlbumStatus.COMPLETED)))
    }

    @Test
    fun `stats add up every kind of decision`() {
        val stats = StatsUiData(totalDeleted = 2, totalKept = 5, totalArchived = 2, totalLocked = 1)
        assertEquals(10, stats.totalSwiped)
        assertEquals(
            mapOf("KEEP" to 0.5f, "DELETE" to 0.2f, "ARCHIVE" to 0.2f, "LOCK" to 0.1f),
            stats.distribution
        )
    }

    @Test
    fun `stats without any decisions have no distribution`() {
        assertTrue(StatsUiData().distribution.isEmpty())
        assertEquals(0, StatsUiData().totalSwiped)
    }
}
