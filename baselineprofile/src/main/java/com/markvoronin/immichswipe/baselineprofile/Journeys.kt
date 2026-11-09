package com.markvoronin.immichswipe.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until

/*
 * The app needs a signed-in Immich account to show albums. The benchmark build installs as its own app
 * (com.markvoronin.immichswipe.benchmark), so sign in to it once by hand; the login survives later runs.
 * The selectors use the English strings, so run with the phone in English.
 */

private const val WAIT_MILLIS = 15_000L

// Long enough for a page transition to settle and the swipe screen to start loading.
private const val SETTLE_MILLIS = 1_500L

val targetAppId: String
    get() = InstrumentationRegistry.getArguments().getString("targetAppId")
        ?.takeIf { it.isNotEmpty() }
        ?: error("Missing targetAppId; run through the baselineprofile module's Gradle tasks.")

fun MacrobenchmarkScope.waitForHome() {
    check(device.wait(Until.hasObject(By.text("Search an album…")), WAIT_MILLIS)) {
        "Home screen not found. Sign in once in the benchmark build ($packageName) before running."
    }
}

fun MacrobenchmarkScope.openAlbumAndBack() = openAndBack(By.text("All Assets"))

fun MacrobenchmarkScope.openDuplicatesAndBack() = openAndBack(By.text("Duplicates"))

fun MacrobenchmarkScope.openSettingsSubmenuAndBack() {
    tap(By.desc("Account"))
    tap(By.desc("Settings"))
    tap(By.text("Interactions"))
    SystemClock.sleep(SETTLE_MILLIS)
    device.pressBack()
    SystemClock.sleep(SETTLE_MILLIS)
    device.pressBack()
    waitForHome()
}

private fun MacrobenchmarkScope.openAndBack(target: BySelector) {
    tap(target)
    SystemClock.sleep(SETTLE_MILLIS)
    device.pressBack()
    waitForHome()
    SystemClock.sleep(SETTLE_MILLIS)
}

private fun MacrobenchmarkScope.tap(selector: BySelector) {
    val node = device.wait(Until.findObject(selector), WAIT_MILLIS) ?: error("Couldn't find $selector")
    node.click()
}
