package com.markvoronin.immichswipe.core.net

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds an account's custom headers to requests that go to its own server. Install it as a network interceptor so
 * it runs for every hop and a redirect to another host never receives them.
 */
class CustomHeadersInterceptor(
    private val baseUrl: String,
    private val headers: Map<String, String>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (headers.isEmpty() || !ImmichAuthInterceptor.isSameOrigin(request.url, baseUrl)) {
            return chain.proceed(request)
        }
        val builder = request.newBuilder()
        headers.forEach { (name, value) -> builder.header(name, value) }
        return chain.proceed(builder.build())
    }
}
