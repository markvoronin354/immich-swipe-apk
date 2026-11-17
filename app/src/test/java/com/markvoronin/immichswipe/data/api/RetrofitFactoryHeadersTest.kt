package com.markvoronin.immichswipe.data.api

import com.markvoronin.immichswipe.core.SessionConfig
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * Sends real requests through the app's API client to a local server, to check which headers actually arrive.
 */
class RetrofitFactoryHeadersTest {

    /** A bare HTTP server on a free local port. It records each request's headers and answers with [respond]. */
    private class LocalServer(private val respond: () -> String) : AutoCloseable {
        val seen = CopyOnWriteArrayList<Map<String, String>>()
        private val socket = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
        val port: Int get() = socket.localPort

        init {
            thread(isDaemon = true) {
                while (!socket.isClosed) {
                    val client = try {
                        socket.accept()
                    } catch (_: IOException) {
                        return@thread
                    }
                    client.use {
                        val reader = it.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                        reader.readLine() ?: return@use
                        val headers = LinkedHashMap<String, String>()
                        while (true) {
                            val line = reader.readLine().orEmpty()
                            if (line.isEmpty()) break
                            headers[line.substringBefore(':').lowercase()] = line.substringAfter(':').trim()
                        }
                        seen += headers
                        it.getOutputStream().write(respond().toByteArray(Charsets.ISO_8859_1))
                    }
                }
            }
        }

        override fun close() = socket.close()
    }

    private val servers = mutableListOf<LocalServer>()

    @After
    fun stopServers() = servers.forEach { it.close() }

    private fun server(respond: () -> String = ::user): LocalServer = LocalServer(respond).also { servers += it }

    private fun user(): String {
        val body = """{"id":"u1","email":"a@b.c","name":"Ann"}"""
        return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${body.length}\r\n" +
            "Connection: close\r\n\r\n$body"
    }

    private fun redirectTo(url: String) = "HTTP/1.1 302 Found\r\nLocation: $url\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"

    private val headers = mapOf("CF-Access-Client-Id" to "abc", "CF-Access-Client-Secret" to "s3cret")

    @Test
    fun `the api client sends the key and the custom headers to its own server`() {
        val own = server()

        val user = runBlocking {
            RetrofitFactory.create(SessionConfig("http://127.0.0.1:${own.port}", "KEY", "u1", headers)).getCurrentUser()
        }

        assertEquals("u1", user.id)
        val request = own.seen.single()
        assertEquals("KEY", request["x-api-key"])
        assertEquals("abc", request["cf-access-client-id"])
        assertEquals("s3cret", request["cf-access-client-secret"])
    }

    @Test
    fun `an account without custom headers sends none`() {
        val own = server()

        runBlocking {
            RetrofitFactory.create(SessionConfig("http://127.0.0.1:${own.port}", "KEY")).getCurrentUser()
        }

        val request = own.seen.single()
        assertEquals("KEY", request["x-api-key"])
        assertNull(request["cf-access-client-id"])
    }

    @Test
    fun `the custom headers do not follow a redirect to another host`() {
        val elsewhere = server()
        val own = server { redirectTo("http://localhost:${elsewhere.port}/api/users/me") }

        runBlocking {
            RetrofitFactory.create(SessionConfig("http://127.0.0.1:${own.port}", "KEY", "u1", headers)).getCurrentUser()
        }

        // The first hop is the account's own server and gets them; the redirect target is somebody else.
        assertEquals("abc", own.seen.single()["cf-access-client-id"])
        val followed = elsewhere.seen.single()
        assertNull(followed["cf-access-client-id"])
        assertNull(followed["cf-access-client-secret"])
    }
}
