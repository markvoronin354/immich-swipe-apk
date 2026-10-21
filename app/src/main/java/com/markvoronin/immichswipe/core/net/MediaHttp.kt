package com.markvoronin.immichswipe.core.net

import android.content.Context
import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.repository.AccountRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one authenticated HTTP client for media: image loading, downloads and sharing.
 * It attaches the API key itself through [ImmichAuthInterceptor].
 */
@Singleton
class MediaHttpClient @Inject constructor(
    private val sessionManager: SessionManager,
    private val accountRepository: AccountRepository
) {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addNetworkInterceptor(ImmichAuthInterceptor(::activeSession, ::accountSession))
        .build()

    fun activeSession(): SessionConfig? = sessionManager.sessionConfig.value

    // Called on an OkHttp worker thread, so blocking on the (rare) database lookup is acceptable.
    private fun accountSession(userId: String): SessionConfig? = runBlocking {
        accountRepository.getAccount(userId)?.let { SessionConfig(it.baseUrl, it.apiKey, it.userId) }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface MediaHttpEntryPoint {
    fun mediaHttpClient(): MediaHttpClient
}

/** Access to [MediaHttpClient] from code that is not created by Hilt, such as composables. */
object MediaHttp {
    private fun holder(context: Context): MediaHttpClient =
        EntryPointAccessors.fromApplication(context.applicationContext, MediaHttpEntryPoint::class.java)
            .mediaHttpClient()

    fun client(context: Context): OkHttpClient = holder(context).client

    fun activeSession(context: Context): SessionConfig? = holder(context).activeSession()
}
