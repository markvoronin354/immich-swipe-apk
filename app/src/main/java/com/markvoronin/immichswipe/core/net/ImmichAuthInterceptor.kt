package com.markvoronin.immichswipe.core.net

import com.markvoronin.immichswipe.core.SessionConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the Immich API key to image, download and similar requests so screens never have to hold or
 * pass the key around.
 *
 * Install it as a network interceptor: it then runs for every hop, redirects included, and re-decides
 * each time. The key is only attached when the request goes to the same origin (scheme, host and port)
 * as the account's server, so it can never be sent to a third party.
 *
 * By default the active session is used. A request that carries [ACCOUNT_HEADER] with a user id is
 * authenticated as that saved account instead, which is how avatars of other accounts load. That header
 * contains no secret and is left on the request so it survives redirects.
 */
class ImmichAuthInterceptor(
    private val activeSession: () -> SessionConfig?,
    private val accountSession: (userId: String) -> SessionConfig?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val accountId = original.header(ACCOUNT_HEADER)
        val session = if (accountId != null) accountSession(accountId) else activeSession()

        // Always drop any pre-set key first, so a stale or foreign key can never ride along on a redirect.
        val builder = original.newBuilder().removeHeader(KEY_HEADER)
        if (session != null && isSameOrigin(original.url, session.baseUrl)) {
            builder.header(KEY_HEADER, session.apiKey)
        }
        return chain.proceed(builder.build())
    }

    companion object {
        const val KEY_HEADER = "x-api-key"
        const val ACCOUNT_HEADER = "x-immich-swipe-account"

        /** True when [url] points at the same scheme, host and port as [baseUrl]. */
        fun isSameOrigin(url: HttpUrl, baseUrl: String): Boolean {
            val base = baseUrl.trim().toHttpUrlOrNull() ?: return false
            return url.scheme == base.scheme &&
                url.host.equals(base.host, ignoreCase = true) &&
                url.port == base.port
        }
    }
}
