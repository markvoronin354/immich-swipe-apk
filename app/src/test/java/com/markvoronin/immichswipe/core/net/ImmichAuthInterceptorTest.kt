package com.markvoronin.immichswipe.core.net

import com.markvoronin.immichswipe.core.SessionConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImmichAuthInterceptorTest {

    private val active = SessionConfig("https://photos.example.com", "ACTIVE-KEY", "user-a")
    private val other = SessionConfig("https://photos.example.com", "OTHER-KEY", "user-b")
    private val elsewhere = SessionConfig("https://other.example.org", "ELSEWHERE-KEY", "user-c")

    /** Runs a request through the interceptor and returns the headers that would reach the network. */
    private fun send(
        url: String,
        headers: Map<String, String> = emptyMap(),
        session: SessionConfig? = active,
        accounts: Map<String, SessionConfig> = mapOf("user-b" to other, "user-c" to elsewhere)
    ): Request {
        var seen: Request? = null
        val terminal = Interceptor { chain ->
            seen = chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("ok").body("".toResponseBody()).build()
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(ImmichAuthInterceptor({ session }, { accounts[it] }))
            .addInterceptor(terminal)
            .build()
        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) -> builder.header(k, v) }
        client.newCall(builder.build()).execute().close()
        return seen!!
    }

    @Test
    fun `active session key is added for the active server`() {
        val req = send("https://photos.example.com/api/assets/1/thumbnail")
        assertEquals("ACTIVE-KEY", req.header("x-api-key"))
    }

    @Test
    fun `no key is ever sent to a different host`() {
        assertNull(send("https://evil.example.net/steal").header("x-api-key"))
        // Same host but another port or scheme is a different origin too.
        assertNull(send("https://photos.example.com:8443/x").header("x-api-key"))
        assertNull(send("http://photos.example.com/x").header("x-api-key"))
    }

    @Test
    fun `host comparison ignores case`() {
        assertEquals("ACTIVE-KEY", send("https://PHOTOS.example.com/x").header("x-api-key"))
    }

    @Test
    fun `a key set by the caller is replaced and never trusted`() {
        val req = send("https://photos.example.com/x", headers = mapOf("x-api-key" to "CALLER-KEY"))
        assertEquals("ACTIVE-KEY", req.header("x-api-key"))
        val foreign = send("https://evil.example.net/x", headers = mapOf("x-api-key" to "CALLER-KEY"))
        assertNull(foreign.header("x-api-key"))
    }

    @Test
    fun `saved account override uses that account's key for its own server`() {
        val req = send(
            "https://photos.example.com/api/users/b/profile-image",
            headers = mapOf(ImmichAuthInterceptor.ACCOUNT_HEADER to "user-b")
        )
        assertEquals("OTHER-KEY", req.header("x-api-key"))
    }

    @Test
    fun `saved account key is not sent to another server`() {
        val req = send(
            "https://photos.example.com/x",
            headers = mapOf(ImmichAuthInterceptor.ACCOUNT_HEADER to "user-c")
        )
        assertNull(req.header("x-api-key"))
    }

    @Test
    fun `unknown account and missing session send no key`() {
        val unknown = send("https://photos.example.com/x", headers = mapOf(ImmichAuthInterceptor.ACCOUNT_HEADER to "nobody"))
        assertNull(unknown.header("x-api-key"))
        assertNull(send("https://photos.example.com/x", session = null).header("x-api-key"))
    }
}
