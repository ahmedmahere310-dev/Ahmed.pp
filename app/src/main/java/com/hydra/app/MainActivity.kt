package com.hydra.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hydra.app.ui.components.HydraBottomNav
import com.hydra.app.ui.screens.HistoryScreen
import com.hydra.app.ui.screens.HomeScreen
import com.hydra.app.ui.screens.SettingsScreen
import com.hydra.app.ui.screens.StatisticsScreen
import com.hydra.app.ui.theme.HydraTheme
import com.hydra.app.ui.viewmodel.HydraViewModel
import com.hydra.app.ui.viewmodel.HydraViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: HydraViewModel by viewModels {
        HydraViewModelFactory(application as HydraApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            HydraTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HydraApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val sampleId = intent?.getLongExtra("EXTRA_SAMPLE_ID", -1L) ?: -1L
        if (sampleId != -1L) {
            viewModel.showFeedbackDialog()
        }
    }
}

@Composable
fun HydraApp(viewModel: HydraViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Handled */ }

        LaunchedEffect(Unit) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            HydraBottomNav(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.setSelectedTab(it) }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> HomeScreen(viewModel = viewModel, uiState = uiState)
                1 -> HistoryScreen(viewModel = viewModel, uiState = uiState)
                2 -> StatisticsScreen(uiState = uiState)
                3 -> SettingsScreen(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}
