package com.starmist.walker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.starmist.walker.tracking.StepService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { StarMistTheme { AppRoot() } }
    }
}

@Composable
private fun AppRoot(vm: AppViewModel = viewModel()) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showDiagnostics by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refresh(announce = true)
    }
    val requestPermission = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
    val openAppSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        )
    }

    val permissionGranted by vm.permissionGranted.collectAsStateWithLifecycle()
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Counting works even if the notification is hidden, so start either way.
        StepService.start(context)
    }

    // Stable mode: keep the foreground service in step with the setting and the permission.
    LaunchedEffect(settings?.stableMode, permissionGranted) {
        val stable = settings?.stableMode ?: return@LaunchedEffect
        if (stable && permissionGranted) {
            val needsNotificationPermission = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsNotificationPermission && !askedNotifications) {
                askedNotifications = true
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                StepService.start(context)
            }
        } else {
            StepService.stop(context)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.startLive() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.stopLive() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }
    BackHandler(enabled = showDiagnostics) { showDiagnostics = false }

    val current = settings
    if (current == null) {
        Box(Modifier.fillMaxSize())
        return
    }

    val replay by vm.replay.collectAsStateWithLifecycle()
    val journey by vm.journey.collectAsStateWithLifecycle()
    replay?.let { summary ->
        ReplayDialog(
            vm = vm,
            settings = current,
            summary = summary,
            regionId = journey?.let { vm.engine.regionAt(it.position)?.id },
            onDismiss = { vm.dismissReplay() },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0 && !showDiagnostics,
                    onClick = { tab = 0; showDiagnostics = false },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("今日") },
                )
                NavigationBarItem(
                    selected = tab == 3 && !showDiagnostics,
                    onClick = { tab = 3; showDiagnostics = false },
                    icon = { Icon(Icons.Filled.Place, contentDescription = null) },
                    label = { Text("旅程") },
                )
                NavigationBarItem(
                    selected = tab == 1 && !showDiagnostics,
                    onClick = { tab = 1; showDiagnostics = false },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    label = { Text("統計") },
                )
                NavigationBarItem(
                    selected = tab == 2 || showDiagnostics,
                    onClick = { tab = 2; showDiagnostics = false },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("設定") },
                )
            }
        },
    ) { padding ->
        when {
            showDiagnostics -> DiagnosticsScreen(vm, padding, openAppSettings)
            tab == 0 -> HomeScreen(vm, current, padding, requestPermission, openAppSettings)
            tab == 1 -> StatsScreen(vm, current, padding)
            tab == 3 -> JourneyScreen(vm, current, padding)
            else -> SettingsScreen(vm, current, padding, onOpenDiagnostics = { showDiagnostics = true })
        }
    }
}
