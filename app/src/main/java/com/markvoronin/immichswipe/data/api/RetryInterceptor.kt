package com.markvoronin.immichswipe.data.api

import com.markvoronin.immichswipe.core.AppLogger
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Retries requests that failed for transient reasons (I/O errors, 429 and 502/503/504) with
 * exponential backoff.
 *
 * Only requests that are safe to replay are retried: a request whose first attempt may already have
 * been applied by the server (creating an album, rotating an asset) is never sent twice. Other client
 * errors such as 401 or 404 are returned immediately because retrying cannot change the outcome.
 */
class RetryInterceptor(
    private val maxAttempts: Int = 3,
    private val baseDelayMs: Long = 500L,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val replaySafe = isReplaySafe(request)
        var attempt = 1

        while (true) {
            val response: Response
            try {
                response = chain.proceed(request)
            } catch (e: IOException) {
                if (!replaySafe || attempt >= maxAttempts || chain.call().isCanceled()) throw e
                backoff(attempt, request, e.message)
                attempt++
                continue
            }

            if (replaySafe && attempt < maxAttempts && response.code in RETRYABLE_STATUS_CODES) {
                response.close()
                backoff(attempt, request, "HTTP ${response.code}")
                attempt++
                continue
            }
            return response
        }
    }

    private fun backoff(attempt: Int, request: Request, reason: String?) {
        val delayMs = baseDelayMs * (1L shl (attempt - 1))
        AppLogger.d(
            "Retrofit",
            "Request ${request.method} ${request.url.encodedPath} failed ($reason). Retrying ${attempt + 1}/$maxAttempts in ${delayMs}ms"
        )
        try {
            sleeper(delayMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Interrupted while waiting to retry", e)
        }
    }

    companion object {
        internal val RETRYABLE_STATUS_CODES = setOf(429, 502, 503, 504)

        internal fun isReplaySafe(request: Request): Boolean = when (request.method) {
            "GET", "HEAD", "OPTIONS", "PUT", "DELETE" -> true
            // Search endpoints use POST but only read data.
            "POST" -> request.url.encodedPath.contains("/api/search/")
            else -> false
        }
    }
}
