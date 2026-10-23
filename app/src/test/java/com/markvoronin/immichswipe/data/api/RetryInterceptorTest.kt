package com.markvoronin.immichswipe.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class RetryInterceptorTest {

    private class Harness(private val script: List<() -> Any>) {
        var calls = 0
        val delays = mutableListOf<Long>()

        // Terminal interceptor that plays the scripted outcomes (a status code or an IOException).
        private val terminal = Interceptor { chain ->
            val step = script[minOf(calls, script.lastIndex)]
            calls++
            when (val outcome = step()) {
                is IOException -> throw outcome
                is Int -> Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(outcome)
                    .message("test")
                    .body("".toResponseBody())
                    .build()
                else -> error("bad script")
            }
        }

        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(maxAttempts = 3, baseDelayMs = 500, sleeper = { delays += it }))
            .addInterceptor(terminal)
            .build()

        fun execute(request: Request): Response = client.newCall(request).execute()
    }

    private fun get(path: String = "/api/albums") = Request.Builder().url("http://localhost$path").build()
    private fun post(path: String) = Request.Builder()
        .url("http://localhost$path")
        .post("{}".toRequestBody())
        .build()

    @Test
    fun `retries transient server errors with exponential backoff`() {
        val h = Harness(listOf({ 503 }, { 503 }, { 200 }))
        assertEquals(200, h.execute(get()).code)
        assertEquals(3, h.calls)
        assertEquals(listOf(500L, 1000L), h.delays)
    }

    @Test
    fun `gives up after max attempts and returns last response`() {
        val h = Harness(listOf { 503 })
        assertEquals(503, h.execute(get()).code)
        assertEquals(3, h.calls)
    }

    @Test
    fun `does not retry authentication or not found errors`() {
        listOf(401, 403, 404).forEach { code ->
            val h = Harness(listOf { code })
            assertEquals(code, h.execute(get()).code)
            assertEquals(1, h.calls)
            assertTrue(h.delays.isEmpty())
        }
    }

    @Test
    fun `retries io errors then succeeds`() {
        val h = Harness(listOf({ IOException("boom") }, { 200 }))
        assertEquals(200, h.execute(get()).code)
        assertEquals(2, h.calls)
    }

    @Test
    fun `rethrows io error after max attempts`() {
        val h = Harness(listOf { IOException("boom") })
        try {
            h.execute(get())
            fail("expected IOException")
        } catch (_: IOException) {
            assertEquals(3, h.calls)
        }
    }

    @Test
    fun `never replays non idempotent posts`() {
        val create = Harness(listOf { 503 })
        assertEquals(503, create.execute(post("/api/albums")).code)
        assertEquals(1, create.calls)

        val rotate = Harness(listOf { IOException("reset") })
        try {
            rotate.execute(post("/api/assets/abc/rotate"))
            fail("expected IOException")
        } catch (_: IOException) {
            assertEquals(1, rotate.calls)
        }
    }

    @Test
    fun `search posts are replay safe`() {
        assertTrue(RetryInterceptor.isReplaySafe(post("/api/search/metadata")))
        assertFalse(RetryInterceptor.isReplaySafe(post("/api/albums")))
        val h = Harness(listOf({ 502 }, { 200 }))
        assertEquals(200, h.execute(post("/api/search/statistics")).code)
        assertEquals(2, h.calls)
    }
}
