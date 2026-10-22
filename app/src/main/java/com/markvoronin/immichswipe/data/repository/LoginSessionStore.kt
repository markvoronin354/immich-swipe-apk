package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Checks that a server URL and API key are valid and returns the user they belong to. */
interface CredentialChecker {
    suspend fun checkCredentials(baseUrl: String, apiKey: String): User
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
    suspend fun saveAccount(baseUrl: String, apiKey: String, user: User)
}
