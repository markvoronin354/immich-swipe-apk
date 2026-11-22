package com.markvoronin.immichswipe

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.feature.auth.AuthScreen
import com.markvoronin.immichswipe.feature.common.LoadingScreen
import com.markvoronin.immichswipe.feature.home.HomeScreen
import com.markvoronin.immichswipe.ui.theme.ImmichSwipeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Phones stay in portrait; tablets and unfolded foldables (smallest width 600dp+) follow the system. */
fun Activity.applyDefaultOrientation() {
    requestedOrientation = if (resources.configuration.smallestScreenWidthDp < 600) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    private var smallestWidthDp = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.i("MainActivity", "Application started")
        applyDefaultOrientation()
        smallestWidthDp = resources.configuration.smallestScreenWidthDp

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val state by appViewModel.uiState.collectAsStateWithLifecycle()


            val useDarkTheme = when (state.themeMode) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            // Follow the in-app theme (not the phone's) for status/navigation bar icon colors.
            DisposableEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { useDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { useDarkTheme },
                )
                onDispose {}
            }

            ImmichSwipeTheme(darkTheme = useDarkTheme, palette = state.palette, customSeed = state.customPaletteSeed, customStyle = state.customPaletteStyle) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        state.isLoading -> {
                            LoadingScreen()
                        }

                        state.isLoggedIn -> {
                            val activeUserId = state.activeUserId
                            val api = sessionManager.api
                            val baseUrl = sessionManager.getBaseUrl()
                            // One-way fingerprint of user, server and key: same change semantics as using the
                            // key itself, without putting the API key into UI state.
                            val fingerprint = sessionManager.sessionFingerprint()

                            if ((api != null) && (baseUrl != null) && (fingerprint != null) && (activeUserId != null)) {
                                val sessionKey = "$activeUserId-$baseUrl-$fingerprint"
                                
                                key(sessionKey) {
                                    HomeScreen(
                                        viewModel = hiltViewModel(key = sessionKey),
                                        sessionKey = sessionKey,
                                        deepLinkIntent = intent
                                    )
                                }
                            } else {
                                LoadingScreen()
                            }
                        }

                        else -> {
                            AuthScreen(
                                viewModel = hiltViewModel()
                            )
                        }
                    }
                }
            }
        }
    }

    // The manifest handles configChanges, so a fold or unfold arrives here instead of recreating the activity.
    // Rotations arrive too, and must not reset the lock the fullscreen viewer lifts.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.smallestScreenWidthDp != smallestWidthDp) {
            smallestWidthDp = newConfig.smallestScreenWidthDp
            applyDefaultOrientation()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
