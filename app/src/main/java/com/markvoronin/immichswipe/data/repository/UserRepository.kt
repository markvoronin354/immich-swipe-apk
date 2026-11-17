package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.util.runCatchingCancellable
import com.markvoronin.immichswipe.data.api.ImmichApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val sessionManager: SessionManager
) {
    private val api: ImmichApi
        get() = sessionManager.api ?: error("No active API session")

    suspend fun getCurrentUser() = api.getCurrentUser()

    /** The permissions the API key was given, or null when the server doesn't say. */
    suspend fun getApiKeyPermissions(): List<String>? =
        runCatchingCancellable { api.getCurrentApiKey().permissions }
            .onFailure { AppLogger.w("UserRepo", "Could not read the API key's permissions: ${it.message}") }
            .getOrNull()
}
