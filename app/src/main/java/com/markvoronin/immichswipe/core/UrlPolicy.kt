package com.markvoronin.immichswipe.core

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Decides whether a server URL needs a warning before the API key is sent to it.
 *
 * Plain HTTP is fine on a private network, which is the main self-hosting use case, so it is allowed
 * silently there. HTTP to a host on the public internet sends the API key unencrypted and requires an
 * explicit confirmation from the user.
 */
object UrlPolicy {

    private val ipv4 = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")

    // Names that only resolve inside a private network or a WireGuard-encrypted tailnet.
    private val privateSuffixes = listOf(".local", ".lan", ".home.arpa", ".internal", ".localdomain", ".ts.net")

    /** True when [url] uses plain HTTP to a host that is not recognisably on a private network. */
    fun isInsecureRemote(url: String): Boolean {
        val parsed = url.trim().toHttpUrlOrNull() ?: return false
        return parsed.scheme == "http" && !isPrivateHost(parsed.host)
    }

    internal fun isPrivateHost(host: String): Boolean {
        val h = host.lowercase().trimEnd('.')
        if (h == "localhost" || (!h.contains('.') && !h.contains(':'))) return true
        if (privateSuffixes.any { h.endsWith(it) }) return true

        ipv4.matchEntire(h)?.let { m ->
            val (a, b) = m.groupValues[1].toInt() to m.groupValues[2].toInt()
            return a == 10 ||
                a == 127 ||
                (a == 172 && b in 16..31) ||
                (a == 192 && b == 168) ||
                (a == 169 && b == 254) ||
                (a == 100 && b in 64..127) // carrier-grade NAT range, also used by Tailscale
        }

        return h.contains(':') && (h == "::1" || h.startsWith("fc") || h.startsWith("fd") ||
                h.startsWith("fe8") || h.startsWith("fe9") || h.startsWith("fea") || h.startsWith("feb"))
        // IPv6 literal: loopback, unique local (fc00::/7) and link local (fe80::/10).
    }
}
