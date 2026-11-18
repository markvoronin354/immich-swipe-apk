package com.markvoronin.immichswipe.data.api

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class MissingPermissionTest {

    private fun http(code: Int, body: String) = HttpException(Response.error<Unit>(code, body.toResponseBody()))

    @Test
    fun `a refusal that names the permission yields it`() {
        // Body as returned by a 3.4.1 server for a key without user.read.
        val refused = http(403, """{"message":"Missing required permission: user.read"}""")

        assertEquals("user.read", refused.missingPermission())
    }

    @Test
    fun `permissions with dots in their names are read whole`() {
        val refused = http(403, """{"message":"Missing required permission: asset.edit.create"}""")

        assertEquals("asset.edit.create", refused.missingPermission())
    }

    @Test
    fun `a refusal that does not name a permission yields nothing`() {
        assertNull(http(403, """{"message":"Forbidden"}""").missingPermission())
        assertNull(http(403, "").missingPermission())
    }

    @Test
    fun `only a 403 counts`() {
        assertNull(http(400, """{"message":"Missing required permission: user.read"}""").missingPermission())
    }

    @Test
    fun `other failures and null yield nothing`() {
        assertNull(IOException("offline").missingPermission())
        assertNull(null.missingPermission())
    }
}
