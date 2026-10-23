package com.markvoronin.immichswipe.feature.settings

import android.content.Context
import android.content.res.Resources
import com.google.gson.Gson
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.ReminderInterval
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.cache.CacheManager
import com.markvoronin.immichswipe.core.reminder.ReminderScheduler
import com.markvoronin.immichswipe.data.local.entity.SwipeDecisionEntity
import com.markvoronin.immichswipe.data.local.entity.SyncHistoryEntity
import com.markvoronin.immichswipe.data.local.entity.UserAccountEntity
import com.markvoronin.immichswipe.data.local.model.DatabaseExport
import com.markvoronin.immichswipe.data.repository.AccountRepository
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.data.repository.UserRepository
import com.markvoronin.immichswipe.domain.model.User
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val reminderEnabled = MutableStateFlow(false)
    private val reminderInterval = MutableStateFlow(ReminderInterval.OFTEN)
    private val reminderMin = MutableStateFlow(3L)
    private val reminderMax = MutableStateFlow(7L)
    private val accounts = MutableStateFlow<List<UserAccountEntity>>(emptyList())

    private val sessionRepository = mockk<SessionRepository>(relaxed = true) {
        every { reminderEnabled } returns this@SettingsViewModelTest.reminderEnabled
        every { reminderInterval } returns this@SettingsViewModelTest.reminderInterval
        every { reminderCustomMinDays } returns reminderMin
        every { reminderCustomMaxDays } returns reminderMax
        every { themeMode } returns MutableStateFlow(AppTheme.DARK)
    }
    private val swipeDecisionRepository = mockk<SwipeDecisionRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true) {
        coEvery { getCurrentUser() } returns User("u1", "a@b.c", "Ann", quotaUsageInBytes = 1234L)
    }
    private val sessionManager = mockk<SessionManager>(relaxed = true) {
        every { getUserId() } returns "u1"
    }
    private val accountRepository = mockk<AccountRepository>(relaxed = true) {
        every { allAccounts } returns accounts
    }
    private val reminderScheduler = mockk<ReminderScheduler>(relaxed = true)

    private val resources = mockk<Resources>()
    private val context = mockk<Context>(relaxed = true) {
        every { resources } returns this@SettingsViewModelTest.resources
        every { getString(any()) } answers { "string:${firstArg<Int>()}" }
        every { getString(any(), *anyVararg()) } answers { "string:${firstArg<Int>()}" }
    }

    private fun decision(id: String, decision: String, user: String = "u1") =
        SwipeDecisionEntity(assetId = id, albumId = "a", userId = user, decision = decision)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { resources.getQuantityString(any(), any(), *anyVararg()) } answers {
            "plural:${firstArg<Int>()}:${secondArg<Int>()}"
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = SettingsViewModel(
        sessionRepository, swipeDecisionRepository, userRepository,
        sessionManager, accountRepository, reminderScheduler
    )

    @Test
    fun `the user name and quota are loaded`() {
        val state = createViewModel().uiState.value
        assertEquals("Ann", state.userName)
        assertEquals(1234L, state.userQuotaBytes)
    }

    @Test
    fun `a user without a name shows an empty name`() {
        coEvery { userRepository.getCurrentUser() } returns User("u1", "a@b.c", null)
        assertEquals("", createViewModel().uiState.value.userName)
    }

    @Test
    fun `a failing user request leaves the defaults`() {
        coEvery { userRepository.getCurrentUser() } throws IOException("offline")
        val state = createViewModel().uiState.value
        assertEquals("", state.userName)
        assertNull(state.userQuotaBytes)
    }

    @Test
    fun `stored settings and the account count are mirrored into the state`() {
        val vm = createViewModel()
        assertEquals(AppTheme.DARK, vm.uiState.value.themeMode)

        accounts.value = listOf(mockk(), mockk())
        assertEquals(2, vm.uiState.value.savedAccountsCount)
    }

    @Test
    fun `setters save through the session repository`() {
        val vm = createViewModel()
        vm.setThemeMode(AppTheme.LIGHT)
        vm.setFullscreenButtonPosition(IconPosition.BOTTOM_LEFT)
        vm.setShowMuteButton(true)
        vm.resetTutorials()

        coVerify { sessionRepository.saveThemeMode(AppTheme.LIGHT) }
        coVerify { sessionRepository.saveFullscreenButtonPosition(IconPosition.BOTTOM_LEFT) }
        coVerify { sessionRepository.saveShowMuteButton(true) }
        coVerify { sessionRepository.resetTutorials() }
    }

    @Test
    fun `turning reminders on schedules them with the current interval`() {
        reminderInterval.value = ReminderInterval.CUSTOM
        reminderMin.value = 2L
        reminderMax.value = 5L
        createViewModel().setReminderEnabled(true)

        coVerify { sessionRepository.saveReminderEnabled(true) }
        verify { reminderScheduler.schedule(ReminderInterval.CUSTOM, 2L, 5L) }
    }

    @Test
    fun `turning reminders off cancels them`() {
        createViewModel().setReminderEnabled(false)

        coVerify { sessionRepository.saveReminderEnabled(false) }
        verify { reminderScheduler.cancel() }
        verify(exactly = 0) { reminderScheduler.schedule(any(), any(), any()) }
    }

    @Test
    fun `changing the interval only reschedules while reminders are on`() {
        val vm = createViewModel()
        vm.setReminderInterval(ReminderInterval.RARELY)
        coVerify { sessionRepository.saveReminderInterval(ReminderInterval.RARELY) }
        verify(exactly = 0) { reminderScheduler.schedule(any(), any(), any()) }

        reminderEnabled.value = true
        vm.setReminderInterval(ReminderInterval.RARELY)
        verify { reminderScheduler.schedule(ReminderInterval.RARELY, 3L, 7L) }
    }

    @Test
    fun `the custom range is made valid before it is saved`() {
        createViewModel().setCustomReminderRange(minDays = 0, maxDays = -4)

        coVerify { sessionRepository.saveReminderCustomMinDays(1L) }
        coVerify { sessionRepository.saveReminderCustomMaxDays(1L) }
    }

    @Test
    fun `a custom range smaller than its start is raised to the start`() {
        createViewModel().setCustomReminderRange(minDays = 10, maxDays = 4)

        coVerify { sessionRepository.saveReminderCustomMinDays(10L) }
        coVerify { sessionRepository.saveReminderCustomMaxDays(10L) }
    }

    @Test
    fun `the custom range reschedules only for enabled custom reminders`() {
        val vm = createViewModel()
        vm.setCustomReminderRange(2, 5)
        verify(exactly = 0) { reminderScheduler.schedule(any(), any(), any()) }

        reminderEnabled.value = true
        reminderInterval.value = ReminderInterval.OFTEN
        vm.setCustomReminderRange(2, 5)
        verify(exactly = 0) { reminderScheduler.schedule(any(), any(), any()) }

        reminderInterval.value = ReminderInterval.CUSTOM
        vm.setCustomReminderRange(2, 5)
        verify { reminderScheduler.schedule(ReminderInterval.CUSTOM, 2L, 5L) }
    }

    @Test
    fun `logging out cancels reminders and ends the session`() {
        createViewModel().logout()

        verify { reminderScheduler.cancel() }
        coVerify { sessionRepository.clearSession() }
    }

    @Test
    fun `removing every account deletes them before ending the session`() {
        createViewModel().removeAllAccounts()

        verify { reminderScheduler.cancel() }
        coVerifyOrder {
            accountRepository.deleteAllAccounts()
            sessionRepository.clearSession()
        }
    }

    @Test
    fun `a database action waits for confirmation and can be dismissed`() {
        val vm = createViewModel()
        vm.requestDatabaseAction(DatabaseAction.DELETE, DatabaseScope.USER)
        assertEquals(DatabaseAction.DELETE, vm.uiState.value.pendingDatabaseAction)
        assertEquals(DatabaseScope.USER, vm.uiState.value.pendingDatabaseScope)

        vm.dismissDatabaseConfirmation()
        assertNull(vm.uiState.value.pendingDatabaseAction)
        assertNull(vm.uiState.value.pendingDatabaseScope)
    }

    @Test
    fun `deleting everything clears all data and reports success`() {
        val vm = createViewModel()
        vm.requestDatabaseAction(DatabaseAction.DELETE, DatabaseScope.ALL)
        vm.executeDelete(DatabaseScope.ALL, context)

        coVerify { swipeDecisionRepository.clearAllData() }
        assertEquals("string:${R.string.settings_db_delete_success}", vm.uiState.value.databaseActionStatus)
        assertNull(vm.uiState.value.pendingDatabaseAction)
    }

    @Test
    fun `deleting for the user clears only the active user`() {
        createViewModel().executeDelete(DatabaseScope.USER, context)

        coVerify { swipeDecisionRepository.clearUserData("u1") }
        coVerify(exactly = 0) { swipeDecisionRepository.clearAllData() }
    }

    @Test
    fun `deleting for the user without a session deletes nothing`() {
        every { sessionManager.getUserId() } returns null
        createViewModel().executeDelete(DatabaseScope.USER, context)

        coVerify(exactly = 0) { swipeDecisionRepository.clearUserData(any()) }
        coVerify(exactly = 0) { swipeDecisionRepository.clearAllData() }
    }

    @Test
    fun `exporting everything writes json without skipped decisions`() {
        val history = SyncHistoryEntity(userId = "u1", deletedCount = 2)
        coEvery { swipeDecisionRepository.getAllDecisionsRaw() } returns listOf(
            decision("a1", "KEEP"), decision("a2", "skip"), decision("a3", "DELETE", user = "u2")
        )
        coEvery { swipeDecisionRepository.getAllSyncHistoryRaw() } returns listOf(history)
        val out = ByteArrayOutputStream()
        val vm = createViewModel()
        vm.requestDatabaseAction(DatabaseAction.EXPORT, DatabaseScope.ALL)

        vm.exportDatabase(DatabaseScope.ALL, out, context)

        val export = Gson().fromJson(out.toString(), DatabaseExport::class.java)
        assertEquals(listOf("a1", "a3"), export.swipeDecisions?.map { it.assetId })
        assertEquals(listOf(history), export.syncHistory)
        assertEquals("ALL", export.scope)
        assertNull(export.userId)
        assertEquals("plural:${R.plurals.settings_db_export_success}:2", vm.uiState.value.databaseActionStatus)
        assertNull(vm.uiState.value.pendingDatabaseAction)
    }

    @Test
    fun `exporting for the user reads only that user's rows`() {
        coEvery { swipeDecisionRepository.getAllDecisionsForUserRaw("u1") } returns listOf(decision("a1", "KEEP"))
        coEvery { swipeDecisionRepository.getAllSyncHistoryForUserRaw("u1") } returns emptyList()
        val out = ByteArrayOutputStream()

        createViewModel().exportDatabase(DatabaseScope.USER, out, context)

        val export = Gson().fromJson(out.toString(), DatabaseExport::class.java)
        assertEquals("USER", export.scope)
        assertEquals("u1", export.userId)
        assertEquals(1, export.swipeDecisions?.size)
        coVerify(exactly = 0) { swipeDecisionRepository.getAllDecisionsRaw() }
    }

    @Test
    fun `a failing export reports the error without touching the file`() {
        coEvery { swipeDecisionRepository.getAllDecisionsRaw() } throws IOException("disk full")
        var closed = false
        val out = object : OutputStream() {
            override fun write(b: Int) = Unit
            override fun close() { closed = true }
        }
        val vm = createViewModel()

        vm.exportDatabase(DatabaseScope.ALL, out, context)

        assertEquals("string:${R.string.settings_db_export_error}", vm.uiState.value.databaseActionStatus)
        assertNull(vm.uiState.value.pendingDatabaseAction)
        // The failure happened before the stream was used, so it is the caller's to close.
        assertFalse(closed)
    }

    @Test
    fun `importing hands the decisions over and clears the skipped counts`() {
        val export = DatabaseExport(
            swipeDecisions = listOf(decision("a1", "KEEP"), decision("a2", "SKIP")),
            syncHistory = listOf(SyncHistoryEntity(userId = "u1", deletedCount = 1, skippedCount = 9))
        )
        val decisions = slot<List<SwipeDecisionEntity>>()
        val history = slot<List<SyncHistoryEntity>>()
        coEvery { swipeDecisionRepository.importData(capture(decisions), capture(history)) } just Runs
        val vm = createViewModel()

        vm.importDatabase(ByteArrayInputStream(Gson().toJson(export).toByteArray()), context)

        assertEquals(listOf("a1", "a2"), decisions.captured.map { it.assetId })
        assertEquals(listOf(0), history.captured.map { it.skippedCount })
        assertEquals(1, history.captured.single().deletedCount)
        // The message counts only the decisions that are not skips.
        assertEquals("plural:${R.plurals.settings_db_import_success}:1", vm.uiState.value.databaseActionStatus)
    }

    @Test
    fun `importing a file that is not a backup reports an error`() {
        val vm = createViewModel()
        vm.importDatabase(ByteArrayInputStream("not json {".toByteArray()), context)

        assertEquals("string:${R.string.settings_db_import_error}", vm.uiState.value.databaseActionStatus)
        coVerify(exactly = 0) { swipeDecisionRepository.importData(any(), any()) }
    }

    @Test
    fun `importing an empty backup imports nothing`() {
        val vm = createViewModel()
        vm.importDatabase(ByteArrayInputStream("{}".toByteArray()), context)

        coVerify { swipeDecisionRepository.importData(emptyList(), emptyList()) }
        assertEquals("plural:${R.plurals.settings_db_import_success}:0", vm.uiState.value.databaseActionStatus)
    }

    @Test
    fun `the status message can be dismissed`() {
        val vm = createViewModel()
        vm.executeDelete(DatabaseScope.ALL, context)
        assertTrue(vm.uiState.value.databaseActionStatus != null)

        vm.clearDatabaseActionStatus()
        assertNull(vm.uiState.value.databaseActionStatus)
    }

    @Test
    fun `dialogs are plain state toggles`() {
        val vm = createViewModel()
        vm.setShowLogs(true)
        vm.setShowActionButtonsDialog(true)
        vm.setShowInteractionsDialog(true)
        vm.setShowClearCacheConfirmation(true)
        vm.setShowCustomReminderDialog(true)

        val state = vm.uiState.value
        assertTrue(state.showLogsDialog && state.showActionButtonsDialog && state.showInteractionsDialog)
        assertTrue(state.showClearCacheConfirmation && state.showCustomReminderDialog)
    }

    @Test
    fun `clearing the cache reports success and closes the confirmation`() {
        mockkObject(CacheManager)
        try {
            every { CacheManager.clearAllCache(context) } just Runs
            val vm = createViewModel()
            vm.setShowClearCacheConfirmation(true)

            vm.clearAppCache(context)

            verify { CacheManager.clearAllCache(context) }
            assertEquals("string:${R.string.settings_clear_cache_success}", vm.uiState.value.databaseActionStatus)
            assertFalse(vm.uiState.value.showClearCacheConfirmation)
        } finally {
            unmockkObject(CacheManager)
        }
    }

    @Test
    fun `a failing cache clear reports the error and closes the confirmation`() {
        mockkObject(CacheManager)
        try {
            every { CacheManager.clearAllCache(context) } throws IOException("locked")
            val vm = createViewModel()
            vm.setShowClearCacheConfirmation(true)

            vm.clearAppCache(context)

            assertEquals("string:${R.string.settings_clear_cache_error}", vm.uiState.value.databaseActionStatus)
            assertFalse(vm.uiState.value.showClearCacheConfirmation)
        } finally {
            unmockkObject(CacheManager)
        }
    }
}
