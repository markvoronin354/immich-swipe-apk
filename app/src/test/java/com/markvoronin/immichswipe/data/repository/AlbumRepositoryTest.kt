package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.testutil.RecordedCall
import com.markvoronin.immichswipe.testutil.fake
import com.markvoronin.immichswipe.testutil.testSessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AlbumRepositoryTest {

    private val album = Album(id = "a1", albumName = "SKIP", assetCount = 0, albumThumbnailAssetId = null)

    private fun repo(handler: (String) -> Any?): Pair<AlbumRepository, MutableList<RecordedCall>> {
        val calls = mutableListOf<RecordedCall>()
        val api = fake<ImmichApi>(calls) { name, _ -> handler(name) }
        return AlbumRepository(testSessionManager(), api) to calls
    }

    @Test
    fun `createAlbum returns the album on success`() = runBlocking {
        val (repo, _) = repo { album }
        assertEquals(album, repo.createAlbum("SKIP").getOrThrow())
    }

    @Test
    fun `createAlbum reports the failure instead of null`() = runBlocking {
        val (repo, _) = repo { throw IllegalStateException("server down") }
        val result = repo.createAlbum("SKIP")
        assertTrue(result.isFailure)
        assertEquals("server down", result.exceptionOrNull()?.message)
    }

    @Test
    fun `adding assets succeeds on a successful response`() = runBlocking {
        val (repo, calls) = repo { Response.success(Unit) }
        assertTrue(repo.addAssetsToAlbum("a1", listOf("x")).isSuccess)
        assertTrue(repo.addAssetToAlbum("a1", "y").isSuccess)
        assertEquals(2, calls.count { it.name == "addAssetsToAlbum" })
    }

    @Test
    fun `an unsuccessful response becomes a failure carrying the http code`() = runBlocking {
        val rejected = Response.error<Unit>(403, "".toResponseBody())
        val (repo, _) = repo { rejected }

        val add = repo.addAssetsToAlbum("a1", listOf("x"))
        assertEquals(403, (add.exceptionOrNull() as HttpException).code())
        val remove = repo.removeAssetsFromAlbum("a1", listOf("x"))
        assertEquals(403, (remove.exceptionOrNull() as HttpException).code())
    }

    @Test
    fun `a thrown error becomes a failure`() = runBlocking {
        val (repo, _) = repo { throw java.io.IOException("offline") }
        assertTrue(repo.addAssetsToAlbum("a1", listOf("x")).isFailure)
        assertTrue(repo.removeAssetsFromAlbum("a1", listOf("x")).isFailure)
    }

    @Test
    fun `cancellation is propagated and not turned into a failure`() {
        val (repo, _) = repo { throw CancellationException("cancelled") }
        try {
            runBlocking { repo.addAssetsToAlbum("a1", listOf("x")) }
            fail("cancellation must not be captured")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }
}
