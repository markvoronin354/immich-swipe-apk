package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.data.local.dao.SwipeDecisionDao
import com.markvoronin.immichswipe.data.local.entity.SwipeDecisionEntity
import com.markvoronin.immichswipe.data.local.entity.SyncHistoryEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class SwipeDecisionRepositoryImportTest {

    private class Call(val name: String, val args: List<Any?>)

    /** Records every DAO call; only the history lookup returns data. */
    private class FakeDao(private val existingHistory: List<SyncHistoryEntity> = emptyList()) {
        val calls = mutableListOf<Call>()

        val dao: SwipeDecisionDao = Proxy.newProxyInstance(
            SwipeDecisionDao::class.java.classLoader,
            arrayOf(SwipeDecisionDao::class.java)
        ) { _, method, args ->
            // Suspend functions get a trailing Continuation argument; drop it.
            val real = (args ?: emptyArray()).toList().dropLast(if (args?.lastOrNull() is kotlin.coroutines.Continuation<*>) 1 else 0)
            calls += Call(method.name, real)
            if (method.name == "getAllSyncHistoryRaw") existingHistory else kotlin.Unit
        } as SwipeDecisionDao

        fun named(name: String) = calls.filter { it.name == name }
    }

    private fun decision(asset: String, user: String, type: String) =
        SwipeDecisionEntity(assetId = asset, albumId = "album", userId = user, decision = type)

    @Test
    fun `import only deletes skip state for the users present in the file`() = runBlocking {
        val fake = FakeDao()
        val repo = SwipeDecisionRepository(fake.dao)

        repo.importData(
            decisions = listOf(
                decision("a1", "userA", "SKIP"),
                decision("a2", "userA", "DELETE")
            ),
            history = emptyList()
        )

        val skipDeletes = fake.named("deleteSkipDecisionsForUser")
        assertEquals(1, skipDeletes.size)
        assertEquals("userA", skipDeletes[0].args[0])

        val assetDeletes = fake.named("deleteDecisionsForAssets")
        assertEquals(1, assetDeletes.size)
        assertEquals("userA", assetDeletes[0].args[0])
        assertEquals(listOf("a1"), assetDeletes[0].args[1])

        // Only the non-skip decision is inserted.
        @Suppress("UNCHECKED_CAST")
        val inserted = fake.named("insertDecisions").single().args[0] as List<SwipeDecisionEntity>
        assertEquals(listOf("a2"), inserted.map { it.assetId })
    }

    @Test
    fun `import never issues an unscoped delete`() = runBlocking {
        val fake = FakeDao()
        SwipeDecisionRepository(fake.dao).importData(
            decisions = listOf(decision("a1", "userA", "SKIP"), decision("b1", "userB", "SKIP")),
            history = emptyList()
        )
        val deletedFor = fake.named("deleteSkipDecisionsForUser").map { it.args[0] }.toSet()
        assertEquals(setOf("userA", "userB"), deletedFor)
        assertTrue(fake.calls.none { it.name == "deleteAllDecisions" || it.name == "deleteAllSyncHistory" })
    }

    @Test
    fun `imported history gets fresh ids and skips rows already present`() = runBlocking {
        val existing = listOf(SyncHistoryEntity(id = 5, userId = "userA", timestamp = 1000L))
        val fake = FakeDao(existingHistory = existing)

        SwipeDecisionRepository(fake.dao).importData(
            decisions = emptyList(),
            history = listOf(
                SyncHistoryEntity(id = 5, userId = "userA", timestamp = 1000L), // duplicate
                SyncHistoryEntity(id = 5, userId = "userB", timestamp = 2000L)  // same id, other user
            )
        )

        @Suppress("UNCHECKED_CAST")
        val inserted = fake.named("insertSyncHistoryList").single().args[0] as List<SyncHistoryEntity>
        assertEquals(1, inserted.size)
        assertEquals("userB", inserted[0].userId)
        assertEquals(0L, inserted[0].id)
    }
}
