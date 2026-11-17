package com.markvoronin.immichswipe.core.net

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomHeadersInterceptorTest {

    private val headers = mapOf("CF-Access-Client-Id" to "abc", "CF-Access-Client-Secret" to "s3cret")

    /** Runs a request through the interceptor and returns what would reach the network. */
    private fun send(
        url: String,
        baseUrl: String = "https://photos.example.com",
        configured: Map<String, String> = headers,
        callerHeaders: Map<String, String> = emptyMap()
    ): Request {
        var seen: Request? = null
        val terminal = Interceptor { chain ->
            seen = chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("ok").body("".toResponseBody()).build()
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(CustomHeadersInterceptor(baseUrl, configured))
            .addInterceptor(terminal)
            .build()
        val builder = Request.Builder().url(url)
        callerHeaders.forEach { (k, v) -> builder.header(k, v) }
        client.newCall(builder.build()).execute().close()
        return seen!!
    }

    @Test
    fun `headers are added for the account's own server`() {
        val request = send("https://photos.example.com/api/users/me")

        assertEquals("abc", request.header("CF-Access-Client-Id"))
        assertEquals("s3cret", request.header("CF-Access-Client-Secret"))
    }

    @Test
    fun `no header is ever sent to a different host, port or scheme`() {
        assertNull(send("https://evil.example.net/steal").header("CF-Access-Client-Secret"))
        assertNull(send("https://photos.example.com:8443/x").header("CF-Access-Client-Secret"))
        assertNull(send("http://photos.example.com/x").header("CF-Access-Client-Secret"))
    }

    @Test
    fun `host comparison ignores case and a trailing slash on the base url`() {
        val request = send("https://PHOTOS.example.com/x", baseUrl = "https://photos.example.com/")

        assertEquals("abc", request.header("CF-Access-Client-Id"))
    }

    @Test
    fun `a header with the same name set by the caller is replaced, not repeated`() {
        val request = send("https://photos.example.com/x", callerHeaders = mapOf("cf-access-client-id" to "old"))

        assertEquals(listOf("abc"), request.headers.values("CF-Access-Client-Id"))
    }

    @Test
    fun `without headers the request is left alone`() {
        val request = send("https://photos.example.com/x", configured = emptyMap(), callerHeaders = mapOf("Accept" to "a"))

        assertEquals("a", request.header("Accept"))
        assertNull(request.header("CF-Access-Client-Id"))
    }
}
