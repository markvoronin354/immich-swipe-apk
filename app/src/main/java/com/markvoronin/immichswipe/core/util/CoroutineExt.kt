package com.markvoronin.immichswipe.core.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Rethrows when this throwable is a coroutine cancellation.
 *
 * A broad `catch (e: Exception)` in suspend code also catches [CancellationException]. Swallowing it
 * keeps a cancelled coroutine running and can make cancellation look like a failure. Call this as the
 * first statement of such a catch block.
 */
fun Throwable.rethrowIfCancelled() {
    if (this is CancellationException) throw this
}

/**
 * Like `runCatching`, but a coroutine cancellation is rethrown instead of being captured as a failure.
 * Use it where a repository wants to report success or failure to its caller as a [Result].
 */
@Suppress("TooGenericExceptionCaught") // Catching everything is the point; cancellation is rethrown below.
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: Exception) {
        e.rethrowIfCancelled()
        Result.failure(e)
    }
