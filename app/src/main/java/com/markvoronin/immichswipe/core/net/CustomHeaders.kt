package com.markvoronin.immichswipe.core.net

/**
 * Extra request headers a user can add to an account, for servers behind a proxy that wants its own, such as
 * Cloudflare Access. Stored as "Name: value" lines.
 */
object CustomHeaders {

    // Set by the app or the HTTP stack itself, so a value from the user would only break requests.
    private val reserved = setOf(
        ImmichAuthInterceptor.KEY_HEADER,
        ImmichAuthInterceptor.ACCOUNT_HEADER,
        "host", "content-length", "connection", "transfer-encoding", "upgrade", "te", "trailer"
    )

    private val tokenName = Regex("[!#\$%&'*+.^_`|~0-9A-Za-z-]+")

    fun isValidName(name: String): Boolean = tokenName.matches(name) && name.lowercase() !in reserved

    // OkHttp refuses anything but visible ASCII, space and tab in a header value.
    fun isValidValue(value: String): Boolean = value.isNotEmpty() && value.all { it == '\t' || it in ' '..'~' }

    /**
     * The headers for these name and value pairs, or null when one is invalid or a name repeats. A row with both
     * parts blank is skipped, so an untouched row is not an error.
     */
    fun fromEntries(entries: List<Pair<String, String>>): Map<String, String>? {
        val result = LinkedHashMap<String, String>()
        for ((rawName, rawValue) in entries) {
            val name = rawName.trim()
            val value = rawValue.trim()
            if (name.isEmpty() && value.isEmpty()) continue
            if (!isValidName(name) || !isValidValue(value)) return null
            if (result.keys.any { it.equals(name, ignoreCase = true) }) return null
            result[name] = value
        }
        return result
    }

    /** The stored form, or null when there are no headers. */
    fun encode(headers: Map<String, String>): String? =
        headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }.ifEmpty { null }

    /** The headers in a stored value; a line that is not a valid header is dropped. */
    fun parse(stored: String?): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        stored.orEmpty().lineSequence().forEach { line ->
            val name = line.substringBefore(':', "").trim()
            val value = line.substringAfter(':', "").trim()
            if (isValidName(name) && isValidValue(value)) result[name] = value
        }
        return result
    }
}
