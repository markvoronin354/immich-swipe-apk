package com.markvoronin.immichswipe.data.api

import retrofit2.HttpException

private val MISSING_PERMISSION = Regex("""Missing required permission: ([\w.]+)""")

/** The permission Immich says a refused request lacked, such as `album.create`; null when it doesn't say. */
fun Throwable?.missingPermission(): String? {
    val refused = (this as? HttpException)?.takeIf { it.code() == 403 } ?: return null
    val body = runCatching { refused.response()?.errorBody()?.string() }.getOrNull() ?: return null
    return MISSING_PERMISSION.find(body)?.groupValues?.get(1)
}
