package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.domain.model.ServerVersion
import com.markvoronin.immichswipe.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Checks that a server URL and API key are valid and returns the user they belong to. */
interface CredentialChecker {
    suspend fun checkCredentials(baseUrl: String, apiKey: String, headers: Map<String, String> = emptyMap()): User

    /** The server's version, or null when it can't be read. Only cancellation is rethrown. */
    suspend fun fetchServerVersion(baseUrl: String, apiKey: String, headers: Map<String, String> = emptyMap()): ServerVersion?

    /** The permissions the API key was given, or null when the server doesn't say. Only cancellation is rethrown. */
    suspend fun fetchApiKeyPermissions(baseUrl: String, apiKey: String, headers: Map<String, String> = emptyMap()): List<String>?
}

/** The part of the saved session the login screen reads and writes. */
interface LoginSessionStore {
    val sessionConfig: Flow<SessionConfig?>
    val savedServerUrls: Flow<List<String>>
    suspend fun saveSession(baseUrl: String, token: String, userId: String)
    suspend fun saveRecentServerUrl(url: String)
    suspend fun removeSavedServerUrl(url: String)
}

/** Stores a logged-in account so it can be switched to later. */
interface AccountSaver {
    suspend fun saveAccount(baseUrl: String, apiKey: String, user: User, headers: Map<String, String> = emptyMap())
}
