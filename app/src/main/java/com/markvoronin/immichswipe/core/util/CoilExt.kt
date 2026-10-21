package com.markvoronin.immichswipe.core.util

import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest

/** Authenticates an Immich image request with the API key header. */
fun ImageRequest.Builder.apiKeyHeader(apiKey: String): ImageRequest.Builder =
    httpHeaders(NetworkHeaders.Builder().add("x-api-key", apiKey).build())
