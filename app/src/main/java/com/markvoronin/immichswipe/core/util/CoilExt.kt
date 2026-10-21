package com.markvoronin.immichswipe.core.util

import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.markvoronin.immichswipe.core.net.ImmichAuthInterceptor

/**
 * Loads this request as a specific saved account instead of the active session. Only needed for
 * content of other accounts, such as their avatars. All other requests are authenticated centrally
 * by [ImmichAuthInterceptor] and need nothing here.
 */
fun ImageRequest.Builder.asAccount(userId: String): ImageRequest.Builder =
    httpHeaders(NetworkHeaders.Builder().add(ImmichAuthInterceptor.ACCOUNT_HEADER, userId).build())
