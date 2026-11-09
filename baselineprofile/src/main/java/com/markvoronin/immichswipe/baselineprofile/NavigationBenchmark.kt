package com.markvoronin.immichswipe.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame times over the main journey (see fullJourney), each run in a fresh process so every screen is composed
 * for the first time, as on a user's first open. Compare the two results to see what the
 * baseline profile buys. Run with `./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class NavigationBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun navigationWithoutCompilation() = navigate(CompilationMode.None())

    @Test
    fun navigationWithBaselineProfile() = navigate(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun navigate(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = targetAppId,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilationMode,
        iterations = 5,
        setupBlock = {
            // A fresh process each time (StartupMode.COLD would instead kill it after this block).
            killProcess()
            startActivityAndWait()
            waitForHome()
        }
    ) {
        fullJourney()
    }
}
