package com.markvoronin.immichswipe.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

/*
 * The app needs a signed-in Immich account to show albums. The benchmark build installs as its own app
 * (com.markvoronin.immichswipe.benchmark), so sign in to it once by hand; the login survives later runs.
 * The selectors use the English strings, so run with the phone in English.
 *
 * Every step leaves the account as it was: swipes are undone before anything could sync, and toggles are
 * flipped back.
 */

private const val WAIT_MILLIS = 15_000L

// The album list can take a while after a fresh start on a slow connection.
private const val HOME_WAIT_MILLIS = 30_000L

// Long enough for a page transition to settle and the swipe screen to start loading.
private const val SETTLE_MILLIS = 1_500L

// Long enough for a card swipe, an undo or a small toggle animation to finish.
private const val SHORT_SETTLE_MILLIS = 800L

val targetAppId: String
    get() = InstrumentationRegistry.getArguments().getString("targetAppId")
        ?.takeIf { it.isNotEmpty() }
        ?: error("Missing targetAppId; run through the baselineprofile module's Gradle tasks.")

fun MacrobenchmarkScope.waitForHome() {
    // The virtual albums' group exists on every account, once the album list has loaded.
    check(device.wait(Until.hasObject(By.text("Special collections")), HOME_WAIT_MILLIS)) {
        "Album list not found. Sign in once in the benchmark build ($packageName) before running."
    }
}

/** The main paths through the app, starting and ending on the home screen. */
fun MacrobenchmarkScope.fullJourney() {
    toggleHomeSectionsAndLayout()
    sortAlbumAndBack()
    openDuplicatesAndBack()
    openSettingsSubmenusAndBack()
}

private fun MacrobenchmarkScope.toggleHomeSectionsAndLayout() {
    // "Not started" only exists while some album is untouched.
    listOf("Not started", "Special collections").filter { device.hasObject(By.text(it)) }.forEach { header ->
        repeat(2) {
            tap(By.text(header))
            SystemClock.sleep(SHORT_SETTLE_MILLIS)
        }
    }
    tap(By.desc("Grid"))
    SystemClock.sleep(SETTLE_MILLIS)
    tap(By.desc("List"))
    SystemClock.sleep(SETTLE_MILLIS)
}

private fun MacrobenchmarkScope.sortAlbumAndBack() {
    tap(By.text("All Assets"))
    // The card's overlay buttons appear once the first asset has loaded.
    check(device.wait(Until.hasObject(By.desc("Fullscreen")), WAIT_MILLIS)) { "The first card never loaded." }

    swipeCard(towardsRight = true)
    tap(By.desc("Undo"))
    SystemClock.sleep(SHORT_SETTLE_MILLIS)
    swipeCard(towardsRight = false)
    tap(By.desc("Undo"))
    SystemClock.sleep(SHORT_SETTLE_MILLIS)

    tap(By.desc("Fullscreen"))
    SystemClock.sleep(SETTLE_MILLIS)
    device.pressBack()
    SystemClock.sleep(SHORT_SETTLE_MILLIS)

    // The progress pill in the header opens the sorting summary.
    tap(By.text(Pattern.compile("\\d+%")))
    SystemClock.sleep(SETTLE_MILLIS)
    device.pressBack()
    SystemClock.sleep(SHORT_SETTLE_MILLIS)

    device.pressBack()
    waitForHome()
    SystemClock.sleep(SETTLE_MILLIS)
}

private fun MacrobenchmarkScope.openDuplicatesAndBack() {
    tap(By.text("Duplicates"))
    SystemClock.sleep(SETTLE_MILLIS)
    device.pressBack()
    waitForHome()
    SystemClock.sleep(SETTLE_MILLIS)
}

private fun MacrobenchmarkScope.openSettingsSubmenusAndBack() {
    tap(By.desc("Account"))
    tap(By.desc("Settings"))
    listOf("Interactions", "Swipe card action buttons").forEach { submenu ->
        tap(By.text(submenu))
        SystemClock.sleep(SETTLE_MILLIS)
        device.pressBack()
        SystemClock.sleep(SETTLE_MILLIS)
    }
    device.pressBack()
    waitForHome()
}

private fun MacrobenchmarkScope.swipeCard(towardsRight: Boolean) {
    val y = device.displayHeight / 2
    val startX = device.displayWidth / 2
    val endX = if (towardsRight) device.displayWidth - 40 else 40
    device.swipe(startX, y, endX, y, 20)
    SystemClock.sleep(SHORT_SETTLE_MILLIS)
}

// Compose can redraw a node between finding and clicking it, so look it up again if it went stale.
private fun MacrobenchmarkScope.tap(selector: BySelector) {
    repeat(3) {
        val node = device.wait(Until.findObject(selector), WAIT_MILLIS) ?: error("Couldn't find $selector")
        try {
            node.click()
            return
        } catch (_: StaleObjectException) {
            SystemClock.sleep(200)
        }
    }
    error("$selector kept changing before it could be tapped")
}
