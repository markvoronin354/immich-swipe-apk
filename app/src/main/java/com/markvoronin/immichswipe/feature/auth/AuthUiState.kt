package com.markvoronin.immichswipe.feature.auth

import com.markvoronin.immichswipe.domain.model.ServerVersion

sealed class AuthError {
    object EmptyFields : AuthError()
    object Dns : AuthError()
    object Timeout : AuthError()
    object Refused : AuthError()
    object Auth : AuthError()
    object Forbidden : AuthError()
    object NotFound : AuthError()
    object Ssl : AuthError()
    object InvalidHeaders : AuthError()
    data class Server(val code: Int) : AuthError()
    data class Unknown(val message: String?) : AuthError()
}

/** One custom header row on the login screen. */
data class HeaderEntry(val name: String = "", val value: String = "")

data class AuthUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    // Extra request headers for a proxy in front of the server.
    val headers: List<HeaderEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val success: Boolean = false,
    val savedServerUrls: List<String> = emptyList(),
    // True while waiting for the user to confirm sending the API key over plain HTTP to a public host.
    val showInsecureHttpWarning: Boolean = false,
    // Set while waiting for the user to continue to a server too old to save photo rotations.
    val outdatedServerVersion: ServerVersion? = null
)
