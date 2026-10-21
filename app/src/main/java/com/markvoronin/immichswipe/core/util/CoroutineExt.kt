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
