package com.markvoronin.immichswipe.domain.model

import com.google.gson.Gson
import com.markvoronin.immichswipe.data.api.ApiKeyResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiKeyPermissionsTest {

    @Test
    fun `a key with only asset_read lacks everything else, in README order`() {
        assertEquals(ApiKeyPermissions.REQUIRED.drop(1), ApiKeyPermissions.missing(listOf("asset.read")))
    }

    @Test
    fun `a key with every required permission lacks nothing`() {
        assertTrue(ApiKeyPermissions.missing(ApiKeyPermissions.REQUIRED).isEmpty())
    }

    @Test
    fun `a key created with all permissions lacks nothing`() {
        assertTrue(ApiKeyPermissions.missing(listOf("all")).isEmpty())
    }

    @Test
    fun `permissions beyond the required ones are fine`() {
        assertTrue(ApiKeyPermissions.missing(ApiKeyPermissions.REQUIRED + "apiKey.read").isEmpty())
    }

    @Test
    fun `the list has no duplicates and includes the permission to create an album`() {
        assertEquals(14, ApiKeyPermissions.REQUIRED.size)
        assertEquals(ApiKeyPermissions.REQUIRED.size, ApiKeyPermissions.REQUIRED.toSet().size)
        assertTrue("album.create" in ApiKeyPermissions.REQUIRED)
    }

    @Test
    fun `the server's api key response parses`() {
        // Shape as returned by a 3.4.1 server for a key that only has asset.read.
        val json = """{"id":"af87137f","name":"API Key","createdAt":"2026-11-17T12:46:47.219Z","updatedAt":"2026-11-17T12:46:47.219Z","permissions":["asset.read"]}"""
        val response = Gson().fromJson(json, ApiKeyResponse::class.java)

        assertEquals(listOf("asset.read"), response.permissions)
    }
}
