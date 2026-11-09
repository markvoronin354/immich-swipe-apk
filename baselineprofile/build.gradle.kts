plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.markvoronin.immichswipe.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        //noinspection OldTargetApi
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    targetProjectPath = ":app"
}

// Run on the connected phone (it needs the one-time login in the benchmark build, see Journeys.kt).
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.uiautomator)
    implementation(libs.benchmark.macro.junit4)
}

// Hands the tested app's id (with its .benchmark suffix) to the tests as the "targetAppId" argument.
androidComponents {
    onVariants { variant ->
        val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { artifactsLoader.load(it)?.applicationId ?: "" }
        )
    }
}
