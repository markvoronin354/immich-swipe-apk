package com.markvoronin.immichswipe.core.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Typed `combine` for six flows. kotlinx.coroutines only offers typed overloads up to five, and the
 * alternative is an untyped array with casts at every call site. The casts live here, once, and are
 * guaranteed correct by the signature.
 */
@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, R> combine6(
    f1: Flow<T1>, f2: Flow<T2>, f3: Flow<T3>, f4: Flow<T4>, f5: Flow<T5>, f6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> = combine(listOf(f1, f2, f3, f4, f5, f6)) { v ->
    transform(v[0] as T1, v[1] as T2, v[2] as T3, v[3] as T4, v[4] as T5, v[5] as T6)
}
