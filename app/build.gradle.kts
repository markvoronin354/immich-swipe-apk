plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.baselineprofile)
}

// Opt-in: ./gradlew assembleRelease -PabiSplits=true builds one small APK per CPU type plus a universal
// one (for GitHub releases). Default builds, the Play bundle and F-Droid are unaffected.
val abiSplitsEnabled = providers.gradleProperty("abiSplits").orNull == "true"

android {
    namespace = "com.markvoronin.immichswipe"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.markvoronin.immichswipe"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 4510
        versionName = "4.5.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (abiSplitsEnabled) {
        splits {
            abi {
                isEnable = true
                reset()
                include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
                isUniversalApk = true
            }
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles (for Google Play)
        includeInBundle = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    lint {
        disable.add("ModifierParameter")
        // The launcher icon paths come from the designer export; simplifying them means redrawing the icon.
        disable.add("VectorPath")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        // android.util.Log etc. return defaults in plain JVM unit tests instead of throwing.
        unitTests.isReturnDefaultValues = true
    }
    sourceSets {
        // Exported Room schemas, used by MigrationTestHelper in instrumented tests.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

if (abiSplitsEnabled) {
    // Split APKs must have distinct, ordered version codes. The scaled scheme is only used for
    // these GitHub builds: base * 10 plus an offset per ABI (0 for the universal APK).
    androidComponents {
        onVariants { variant ->
            val offsets = mapOf("x86" to 1, "x86_64" to 2, "armeabi-v7a" to 3, "arm64-v8a" to 4)
            variant.outputs.forEach { output ->
                val abi = output.filters
                    .firstOrNull { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }
                    ?.identifier
                output.versionCode.set((output.versionCode.orNull ?: 0) * 10 + (offsets[abi] ?: 0))
            }
        }
    }
}

// The baseline profile plugin's build types (profile generation and benchmarks) install next to the real app
// as com.markvoronin.immichswipe.benchmark, signed with the debug key; release builds are unchanged.
androidComponents {
    finalizeDsl { android ->
        listOf("nonMinifiedRelease", "benchmarkRelease").forEach { name ->
            android.buildTypes.findByName(name)?.apply {
                applicationIdSuffix = ".benchmark"
                signingConfig = android.signingConfigs.getByName("debug")
            }
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    baseline = rootProject.file("config/detekt/baseline.xml")
    source.setFrom("src/main/java", "src/test/java")
}

ksp {
    // Export Room schemas so future migrations can be verified against them.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Manually added
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.video) // For video thumbnails
    implementation(libs.coil.gif)
    implementation(libs.coil.network.okhttp) // Fetches images over HTTP (not bundled in Coil 3)

    // Media3 for video playback
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Baseline Profile: generated by the :baselineprofile module into src/release/generated/ and shipped in
    // release builds (see BaselineProfileGenerator there).
    baselineProfile(project(":baselineprofile"))
    androidTestImplementation(libs.room.testing)

    // ML for "Best Shot" feature (FOSS-friendly)
    implementation(libs.mediapipe.tasks.vision)

    // Liquid glass backdrop effect for the swipe metadata panel
    implementation(libs.backdrop)
}
