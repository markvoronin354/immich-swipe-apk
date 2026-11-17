package com.markvoronin.immichswipe.feature.auth

import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.data.repository.AccountSaver
import com.markvoronin.immichswipe.data.repository.CredentialChecker
import com.markvoronin.immichswipe.data.repository.LoginSessionStore
import com.markvoronin.immichswipe.domain.model.ApiKeyPermissions
import com.markvoronin.immichswipe.domain.model.ServerVersion
import com.markvoronin.immichswipe.domain.model.User
import com.markvoronin.immichswipe.testutil.testSessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private class FakeChecker(var failWith: Throwable? = null) : CredentialChecker {
        var version: ServerVersion? = null
        var permissions: List<String>? = null
        val checked = mutableListOf<Pair<String, String>>()
        val headersSent = mutableListOf<Map<String, String>>()
        override suspend fun fetchServerVersion(baseUrl: String, apiKey: String, headers: Map<String, String>) = version
        override suspend fun fetchApiKeyPermissions(baseUrl: String, apiKey: String, headers: Map<String, String>) = permissions
        override suspend fun checkCredentials(baseUrl: String, apiKey: String, headers: Map<String, String>): User {
            checked += baseUrl to apiKey
            headersSent += headers
            failWith?.let { throw it }
            return User(id = "u1", email = "a@b.c", name = "Ann")
        }
    }

    private class FakeStore : LoginSessionStore {
        override val sessionConfig = MutableStateFlow<SessionConfig?>(null)
        override val savedServerUrls = MutableStateFlow<List<String>>(emptyList())
        val sessions = mutableListOf<Triple<String, String, String>>()
        val recentUrls = mutableListOf<String>()
        val removedUrls = mutableListOf<String>()
        override suspend fun saveSession(baseUrl: String, token: String, userId: String) { sessions += Triple(baseUrl, token, userId) }
        override suspend fun saveRecentServerUrl(url: String) { recentUrls += url }
        override suspend fun removeSavedServerUrl(url: String) { removedUrls += url }
    }

    private class FakeAccounts : AccountSaver {
        val saved = mutableListOf<Triple<String, String, String>>()
        val savedHeaders = mutableListOf<Map<String, String>>()
        override suspend fun saveAccount(baseUrl: String, apiKey: String, user: User, headers: Map<String, String>) {
            saved += Triple(baseUrl, apiKey, user.id)
            savedHeaders += headers
        }
    }

    private val checker = FakeChecker()
    private val store = FakeStore()
    private val accounts = FakeAccounts()
    private lateinit var vm: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = AuthViewModel(store, checker, accounts, testSessionManager())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun enter(url: String, key: String = "key") {
        vm.onBaseUrlChange(url)
        vm.onApiKeyChange(key)
    }

    @Test
    fun `empty fields report an error and contact nobody`() {
        vm.login()
        assertEquals(AuthError.EmptyFields, vm.uiState.value.error)
        assertTrue(checker.checked.isEmpty())

        vm.onBaseUrlChange("https://photos.example.com")
        vm.login()
        assertEquals(AuthError.EmptyFields, vm.uiState.value.error)
        assertTrue(checker.checked.isEmpty())
    }

    @Test
    fun `a valid login saves the account and the session`() {
        enter("https://photos.example.com", " secret ")
        vm.login()

        // Input is trimmed before use.
        assertEquals(listOf("https://photos.example.com" to "secret"), checker.checked)
        assertEquals(listOf(Triple("https://photos.example.com", "secret", "u1")), accounts.saved)
        assertEquals(listOf(Triple("https://photos.example.com", "secret", "u1")), store.sessions)
        assertEquals(listOf("https://photos.example.com"), store.recentUrls)
        assertTrue(vm.uiState.value.success)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `custom headers are sent with the check and saved with the account`() {
        enter("https://photos.example.com")
        vm.onAddHeader()
        vm.onHeaderChange(0, HeaderEntry(" CF-Access-Client-Id ", " abc "))
        vm.onAddHeader()
        vm.onHeaderChange(1, HeaderEntry("CF-Access-Client-Secret", "s3cret"))
        vm.login()

        val expected = mapOf("CF-Access-Client-Id" to "abc", "CF-Access-Client-Secret" to "s3cret")
        assertEquals(listOf(expected), checker.headersSent)
        assertEquals(listOf(expected), accounts.savedHeaders)
        assertTrue(vm.uiState.value.success)
    }

    @Test
    fun `a login without headers sends none and a blank row is ignored`() {
        enter("https://photos.example.com")
        vm.onAddHeader()
        vm.login()

        assertEquals(listOf(emptyMap<String, String>()), checker.headersSent)
        assertTrue(vm.uiState.value.success)
    }

    @Test
    fun `an invalid header reports an error and contacts nobody`() {
        enter("https://photos.example.com")
        vm.onAddHeader()
        vm.onHeaderChange(0, HeaderEntry("bad name", "value"))
        vm.login()

        assertEquals(AuthError.InvalidHeaders, vm.uiState.value.error)
        assertTrue(checker.checked.isEmpty())
        assertTrue(accounts.saved.isEmpty())
    }

    @Test
    fun `a header name the app sets itself is rejected`() {
        enter("https://photos.example.com")
        vm.onAddHeader()
        vm.onHeaderChange(0, HeaderEntry("X-Api-Key", "other"))
        vm.login()

        assertEquals(AuthError.InvalidHeaders, vm.uiState.value.error)
        assertTrue(checker.checked.isEmpty())
    }

    @Test
    fun `header rows can be edited and removed`() {
        vm.onAddHeader()
        vm.onAddHeader()
        vm.onHeaderChange(0, HeaderEntry("A", "1"))
        vm.onHeaderChange(1, HeaderEntry("B", "2"))
        vm.onHeaderChange(7, HeaderEntry("ignored", "x"))
        vm.onRemoveHeader(0)
        vm.onRemoveHeader(9)

        assertEquals(listOf(HeaderEntry("B", "2")), vm.uiState.value.headers)
    }

    @Test
    fun `adding another account starts without headers`() {
        vm.onAddHeader()
        vm.onHeaderChange(0, HeaderEntry("A", "1"))
        vm.prepareForAddAccount()

        assertTrue(vm.uiState.value.headers.isEmpty())
    }

    @Test
    fun `http to a public host asks for confirmation before any request`() {
        enter("http://203.0.113.10:2283")
        vm.login()

        assertTrue(vm.uiState.value.showInsecureHttpWarning)
        assertTrue(checker.checked.isEmpty())
        assertFalse(vm.uiState.value.success)
    }

    @Test
    fun `confirming the warning continues the login`() {
        enter("http://203.0.113.10:2283")
        vm.login()
        vm.login(allowInsecureHttp = true)

        assertEquals(1, checker.checked.size)
        assertFalse(vm.uiState.value.showInsecureHttpWarning)
        assertTrue(vm.uiState.value.success)
    }

    @Test
    fun `cancelling the warning clears it without logging in`() {
        enter("http://203.0.113.10:2283")
        vm.login()
        vm.dismissInsecureHttpWarning()

        assertFalse(vm.uiState.value.showInsecureHttpWarning)
        assertTrue(checker.checked.isEmpty())
    }

    @Test
    fun `http on a private address connects without a warning`() {
        enter("http://192.168.1.5:2283")
        vm.login()

        assertFalse(vm.uiState.value.showInsecureHttpWarning)
        assertTrue(vm.uiState.value.success)
    }

    @Test
    fun `a server older than the rotation minimum pauses the login before anything is saved`() {
        checker.version = ServerVersion(2, 4, 1)
        enter("https://photos.example.com")
        vm.login()

        assertEquals(ServerVersion(2, 4, 1), vm.uiState.value.outdatedServerVersion)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.success)
        assertTrue(accounts.saved.isEmpty())
        assertTrue(store.sessions.isEmpty())
    }

    @Test
    fun `continuing with an old server finishes the login`() {
        checker.version = ServerVersion(2, 4, 1)
        enter("https://photos.example.com")
        vm.login()
        vm.continueDespiteWarnings()

        assertNull(vm.uiState.value.outdatedServerVersion)
        assertEquals(1, accounts.saved.size)
        assertEquals(1, store.sessions.size)
        assertTrue(vm.uiState.value.success)
    }

    @Test
    fun `cancelling the old server warning clears it without logging in`() {
        checker.version = ServerVersion(2, 4, 1)
        enter("https://photos.example.com")
        vm.login()
        vm.dismissServerWarnings()

        assertNull(vm.uiState.value.outdatedServerVersion)
        assertTrue(accounts.saved.isEmpty())
        assertFalse(vm.uiState.value.success)
    }

    @Test
    fun `an old server behind a confirmed http address is not asked about http again`() {
        checker.version = ServerVersion(2, 4, 1)
        enter("http://203.0.113.10:2283")
        vm.login()
        vm.login(allowInsecureHttp = true)
        vm.continueDespiteWarnings()

        assertFalse(vm.uiState.value.showInsecureHttpWarning)
        assertNull(vm.uiState.value.outdatedServerVersion)
        assertTrue(vm.uiState.value.success)
    }

    @Test
    fun `a server at the rotation minimum logs in without a warning`() {
        checker.version = ServerVersion.ROTATION_EDITS
        enter("https://photos.example.com")
        vm.login()

        assertNull(vm.uiState.value.outdatedServerVersion)
        assertTrue(vm.uiState.value.success)
    }

    @Test
    fun `a version that can't be read logs in without a warning`() {
        checker.version = null
        enter("https://photos.example.com")
        vm.login()

        assertNull(vm.uiState.value.outdatedServerVersion)
        assertTrue(vm.uiState.value.success)
    }

    @Test
    fun `a key missing permissions pauses the login and lists them`() {
        checker.permissions = listOf("asset.read", "user.read")
        enter("https://photos.example.com")
        vm.login()

        val missing = ApiKeyPermissions.REQUIRED - setOf("asset.read", "user.read")
        assertEquals(missing, vm.uiState.value.missingPermissions)
        assertNull(vm.uiState.value.outdatedServerVersion)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.success)
        assertTrue(accounts.saved.isEmpty())
        assertTrue(store.sessions.isEmpty())
    }

    @Test
    fun `continuing with a key missing permissions finishes the login`() {
        checker.permissions = listOf("asset.read", "user.read")
        enter("https://photos.example.com")
        vm.login()
        vm.continueDespiteWarnings()

        assertTrue(vm.uiState.value.missingPermissions.isEmpty())
        assertEquals(1, accounts.saved.size)
        assertTrue(vm.uiState.value.success)
    }

    @Test
    fun `an old server and a key missing permissions share one pause`() {
        checker.version = ServerVersion(2, 4, 1)
        checker.permissions = listOf("asset.read")
        enter("https://photos.example.com")
        vm.login()

        assertEquals(ServerVersion(2, 4, 1), vm.uiState.value.outdatedServerVersion)
        assertEquals(ApiKeyPermissions.REQUIRED - "asset.read", vm.uiState.value.missingPermissions)

        vm.continueDespiteWarnings()
        assertTrue(vm.uiState.value.success)
    }

    @Test
    fun `cancelling the warnings clears them without logging in`() {
        checker.version = ServerVersion(2, 4, 1)
        checker.permissions = listOf("asset.read")
        enter("https://photos.example.com")
        vm.login()
        vm.dismissServerWarnings()

        assertNull(vm.uiState.value.outdatedServerVersion)
        assertTrue(vm.uiState.value.missingPermissions.isEmpty())
        assertTrue(accounts.saved.isEmpty())
    }

    @Test
    fun `a key with every permission, or with all, logs in without a warning`() {
        checker.permissions = ApiKeyPermissions.REQUIRED
        enter("https://photos.example.com")
        vm.login()
        assertTrue(vm.uiState.value.success)
        assertTrue(vm.uiState.value.missingPermissions.isEmpty())

        vm.resetState()
        checker.permissions = listOf("all")
        enter("https://photos.example.com")
        vm.login()
        assertTrue(vm.uiState.value.success)
        assertTrue(vm.uiState.value.missingPermissions.isEmpty())
    }

    @Test
    fun `permissions that can't be read log in without a warning`() {
        checker.permissions = null
        enter("https://photos.example.com")
        vm.login()

        assertTrue(vm.uiState.value.missingPermissions.isEmpty())
        assertTrue(vm.uiState.value.success)
    }

    @Test
    fun `a refused login names the permissions the key lacks`() {
        checker.failWith = http(403)
        checker.permissions = listOf("asset.read")
        enter("https://photos.example.com")
        vm.login()

        assertEquals(AuthError.Forbidden(ApiKeyPermissions.REQUIRED - "asset.read"), vm.uiState.value.error)
        assertTrue(accounts.saved.isEmpty())
    }

    @Test
    fun `a refused login stays generic when the key's permissions can't be read`() {
        checker.failWith = http(403)
        checker.permissions = null
        enter("https://photos.example.com")
        vm.login()

        assertEquals(AuthError.Forbidden(), vm.uiState.value.error)
    }

    @Test
    fun `connection failures map to the matching error`() {
        fun errorFor(failure: Throwable): AuthError? {
            checker.failWith = failure
            enter("https://photos.example.com")
            vm.login()
            return vm.uiState.value.error
        }

        assertEquals(AuthError.Dns, errorFor(UnknownHostException()))
        assertEquals(AuthError.Timeout, errorFor(SocketTimeoutException()))
        assertEquals(AuthError.Refused, errorFor(ConnectException()))
        assertEquals(AuthError.Ssl, errorFor(SSLHandshakeException("bad cert")))
        assertEquals(AuthError.Auth, errorFor(http(401)))
        assertEquals(AuthError.Forbidden(), errorFor(http(403)))
        assertEquals(AuthError.NotFound, errorFor(http(404)))
        assertEquals(AuthError.Server(502), errorFor(http(502)))
        assertEquals(AuthError.Unknown("boom"), errorFor(IllegalStateException("boom")))
    }

    @Test
    fun `a failed login saves nothing and stops loading`() {
        checker.failWith = http(401)
        enter("https://photos.example.com")
        vm.login()

        assertTrue(accounts.saved.isEmpty())
        assertTrue(store.sessions.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.success)
    }

    @Test
    fun `cancellation is not reported as a login error`() {
        checker.failWith = CancellationException("left the screen")
        enter("https://photos.example.com")
        vm.login()

        assertNull(vm.uiState.value.error)
        assertTrue(accounts.saved.isEmpty())
    }

    @Test
    fun `saved server urls are shown and can be removed`() {
        store.savedServerUrls.value = listOf("https://a.example.com")
        assertEquals(listOf("https://a.example.com"), vm.uiState.value.savedServerUrls)

        vm.removeSavedServerUrl("https://a.example.com")
        assertEquals(listOf("https://a.example.com"), store.removedUrls)
    }

    @Test
    fun `logging out resets the form but keeps the saved urls`() {
        store.savedServerUrls.value = listOf("https://a.example.com")
        enter("https://photos.example.com")

        // A session existing and then disappearing is how a logout reaches the screen.
        store.sessionConfig.value = SessionConfig("https://photos.example.com", "k", "u1")
        store.sessionConfig.value = null

        assertEquals("", vm.uiState.value.baseUrl)
        assertEquals("", vm.uiState.value.apiKey)
        assertEquals(listOf("https://a.example.com"), vm.uiState.value.savedServerUrls)
    }

    private fun http(code: Int) = HttpException(Response.error<Unit>(code, "".toResponseBody()))
}
