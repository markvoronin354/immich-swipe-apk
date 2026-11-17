package com.markvoronin.immichswipe.domain.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerVersionTest {

    @Test
    fun `versions compare by number, not by text`() {
        assertTrue(ServerVersion(2, 4, 9) < ServerVersion.ROTATION_EDITS)
        assertTrue(ServerVersion(2, 10, 0) > ServerVersion.ROTATION_EDITS)
        assertTrue(ServerVersion(1, 99, 99) < ServerVersion.ROTATION_EDITS)
        assertTrue(ServerVersion(3, 0, 0) > ServerVersion.ROTATION_EDITS)
        assertTrue(ServerVersion(2, 5, 1) > ServerVersion.ROTATION_EDITS)
        assertEquals(0, ServerVersion(2, 5, 0).compareTo(ServerVersion.ROTATION_EDITS))
    }

    @Test
    fun `the rotation minimum is 2_5_0`() {
        assertEquals("2.5.0", ServerVersion.ROTATION_EDITS.toString())
    }

    @Test
    fun `the server's version response parses`() {
        // Shape as returned by a 3.4.1 server, including the fields this app ignores.
        val json = """{"major":3,"minor":4,"patch":1,"prerelease":null,"featureLevel":2}"""
        val version = Gson().fromJson(json, ServerVersion::class.java)

        assertEquals(ServerVersion(3, 4, 1), version)
    }
}
