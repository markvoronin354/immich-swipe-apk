package com.markvoronin.immichswipe.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records which code the main journeys run, so release builds ship it precompiled.
 * Run with `./gradlew :app:generateReleaseBaselineProfile`; the result lands in app/src/release/generated/.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = targetAppId, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        waitForHome()
        openAlbumAndBack()
        openDuplicatesAndBack()
        openSettingsSubmenuAndBack()
    }
}
