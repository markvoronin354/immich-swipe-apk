package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.core.util.runCatchingCancellable
import com.markvoronin.immichswipe.data.api.RetrofitFactory
import com.markvoronin.immichswipe.domain.model.ServerVersion
import com.markvoronin.immichswipe.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthRepository @Inject constructor() : CredentialChecker {


    override suspend fun checkCredentials(baseUrl: String, apiKey: String, headers: Map<String, String>): User {
        val config = SessionConfig(baseUrl = baseUrl, apiKey = apiKey, headers = headers)
        
        val tempApi = RetrofitFactory.create(config)
        
        return tempApi.getCurrentUser()
    }

    override suspend fun fetchServerVersion(baseUrl: String, apiKey: String, headers: Map<String, String>): ServerVersion? {
        val config = SessionConfig(baseUrl = baseUrl, apiKey = apiKey, headers = headers)
        return runCatchingCancellable { RetrofitFactory.create(config).getServerVersion() }
            .onFailure { AppLogger.w("Auth", "Could not read the server version: ${it.message}") }
            .getOrNull()
    }
}
