package com.markvoronin.immichswipe.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.UrlPolicy
import com.markvoronin.immichswipe.core.net.CustomHeaders
import com.markvoronin.immichswipe.core.util.rethrowIfCancelled
import com.markvoronin.immichswipe.data.repository.AccountSaver
import com.markvoronin.immichswipe.data.repository.CredentialChecker
import com.markvoronin.immichswipe.data.repository.LoginSessionStore
import com.markvoronin.immichswipe.domain.model.ApiKeyPermissions
import com.markvoronin.immichswipe.domain.model.ServerVersion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sessionRepository: LoginSessionStore,
    private val authRepository: CredentialChecker,
    private val accountRepository: AccountSaver,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {

        observeSessionReset()
        observeSavedServerUrls()
    }

    private fun observeSessionReset() {
        viewModelScope.launch {
            sessionRepository.sessionConfig.collect { config ->
                if (config == null) {
                    resetState()
                }
            }
        }
    }

    private fun observeSavedServerUrls() {
        viewModelScope.launch {
            sessionRepository.savedServerUrls.collect { urls ->
                _uiState.value = _uiState.value.copy(savedServerUrls = urls)
            }
        }
    }

    fun onBaseUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(baseUrl = value)
    }

    fun onApiKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value)
    }

    fun onAddHeader() {
        _uiState.value = _uiState.value.copy(headers = _uiState.value.headers + HeaderEntry())
    }

    fun onHeaderChange(index: Int, entry: HeaderEntry) {
        val headers = _uiState.value.headers
        if (index !in headers.indices) return
        _uiState.value = _uiState.value.copy(headers = headers.toMutableList().also { it[index] = entry })
    }

    fun onRemoveHeader(index: Int) {
        val headers = _uiState.value.headers
        if (index !in headers.indices) return
        _uiState.value = _uiState.value.copy(headers = headers.toMutableList().also { it.removeAt(index) })
    }

    fun removeSavedServerUrl(url: String) {
        viewModelScope.launch {
            sessionRepository.removeSavedServerUrl(url)
        }
    }


    fun resetState() {
        val currentSavedUrls = _uiState.value.savedServerUrls
        _uiState.value = AuthUiState(savedServerUrls = currentSavedUrls)
    }


    fun clearAllFields() {
        val currentSavedUrls = _uiState.value.savedServerUrls
        _uiState.value = AuthUiState(savedServerUrls = currentSavedUrls)
    }


    fun prepareForAddAccount(defaultBaseUrl: String? = null) {
        val initialUrl = defaultBaseUrl?.takeIf { it.isNotEmpty() } ?: _uiState.value.baseUrl
        _uiState.value = _uiState.value.copy(
            baseUrl = initialUrl,
            apiKey = "",
            headers = emptyList(),
            isLoading = false,
            error = null,
            success = false
        )
    }


    fun dismissInsecureHttpWarning() {
        _uiState.value = _uiState.value.copy(showInsecureHttpWarning = false)
    }

    fun dismissServerWarnings() {
        _uiState.value = _uiState.value.copy(outdatedServerVersion = null, missingPermissions = emptyList())
    }

    // The plain-HTTP check already ran, or was confirmed, before the server was first contacted.
    fun continueDespiteWarnings() = login(allowInsecureHttp = true, acknowledgeWarnings = true)

    // Best effort: a server that doesn't say what the key can do counts as missing nothing.
    private suspend fun missingPermissions(baseUrl: String, apiKey: String, headers: Map<String, String>): List<String> {
        val granted = authRepository.fetchApiKeyPermissions(baseUrl, apiKey, headers) ?: return emptyList()
        return ApiKeyPermissions.missing(granted).also {
            if (it.isNotEmpty()) AppLogger.w("Auth", "API key lacks: ${it.joinToString()}")
        }
    }


    fun login(allowInsecureHttp: Boolean = false, acknowledgeWarnings: Boolean = false) {
        viewModelScope.launch {
            val baseUrl = _uiState.value.baseUrl.trim()
            val apiKey = _uiState.value.apiKey.trim()
            val headers = CustomHeaders.fromEntries(_uiState.value.headers.map { it.name to it.value })

            AppLogger.i("Auth", "Attempting connection to $baseUrl")

            if (baseUrl.isEmpty() || apiKey.isEmpty()) {
                _uiState.value = _uiState.value.copy(error = AuthError.EmptyFields)
                return@launch
            }
            if (headers == null) {
                _uiState.value = _uiState.value.copy(error = AuthError.InvalidHeaders)
                return@launch
            }

            // Plain HTTP to a public host would send the API key unencrypted: ask first.
            if (!allowInsecureHttp && UrlPolicy.isInsecureRemote(baseUrl)) {
                _uiState.value = _uiState.value.copy(showInsecureHttpWarning = true, error = null)
                return@launch
            }
            _uiState.value = _uiState.value.copy(showInsecureHttpWarning = false)

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                outdatedServerVersion = null,
                missingPermissions = emptyList()
            )

            try {
                val user = authRepository.checkCredentials(baseUrl, apiKey, headers)
                AppLogger.i("Auth", "Valid credentials. User: ${user.name} (${user.id})")

                val version = authRepository.fetchServerVersion(baseUrl, apiKey, headers)
                AppLogger.i("Auth", "Server version: ${version ?: "unknown"}")
                val oldVersion = version?.takeIf { it < ServerVersion.ROTATION_EDITS }
                if (oldVersion != null) {
                    AppLogger.w("Auth", "Server $oldVersion is older than ${ServerVersion.ROTATION_EDITS}: photo rotations can't be saved to it")
                }
                val missing = missingPermissions(baseUrl, apiKey, headers)
                val hasWarnings = oldVersion != null || missing.isNotEmpty()
                if (hasWarnings && !acknowledgeWarnings) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        outdatedServerVersion = oldVersion,
                        missingPermissions = missing
                    )
                    return@launch
                }

                val config = SessionConfig(baseUrl = baseUrl, apiKey = apiKey, userId = user.id, headers = headers)
                sessionManager.initialize(config)

                accountRepository.saveAccount(baseUrl, apiKey, user, headers)
                sessionRepository.saveSession(baseUrl = baseUrl, token = apiKey, userId = user.id)
                sessionRepository.saveRecentServerUrl(baseUrl)

                _uiState.value = _uiState.value.copy(isLoading = false, success = true)

            } catch (e: Exception) {
                e.rethrowIfCancelled()
                val error = when (e) {
                    is java.net.UnknownHostException -> AuthError.Dns
                    is java.net.SocketTimeoutException -> AuthError.Timeout
                    is java.net.ConnectException -> AuthError.Refused
                    is retrofit2.HttpException -> {
                        when (e.code()) {
                            401 -> AuthError.Auth
                            403 -> AuthError.Forbidden(missingPermissions(baseUrl, apiKey, headers))
                            404 -> AuthError.NotFound
                            else -> AuthError.Server(e.code())
                        }
                    }
                    is javax.net.ssl.SSLHandshakeException -> AuthError.Ssl
                    else -> AuthError.Unknown(e.localizedMessage)
                }

                AppLogger.e("Auth", "Connection failed: $error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error
                )
            }
        }
    }
}
