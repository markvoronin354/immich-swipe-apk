package com.markvoronin.immichswipe.feature.auth

import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.data.repository.AccountSaver
import com.markvoronin.immichswipe.data.repository.CredentialChecker
import com.markvoronin.immichswipe.data.repository.LoginSessionStore
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

    private val user = User(id = "u1", email = "a@b.c", name = "Ann")

    private class FakeChecker(var failWith: Throwable? = null) : CredentialChecker {
        val checked = mutableListOf<Pair<String, String>>()
        override suspend fun checkCredentials(baseUrl: String, apiKey: String): User {
            checked += baseUrl to apiKey
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
        override suspend fun saveAccount(baseUrl: String, apiKey: String, user: User) { saved += Triple(baseUrl, apiKey, user.id) }
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
        assertEquals(AuthError.Forbidden, errorFor(http(403)))
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
