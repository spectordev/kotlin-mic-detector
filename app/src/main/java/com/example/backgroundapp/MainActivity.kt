package com.example.backgroundapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.backgroundapp.data.MonitorSettings
import com.example.backgroundapp.data.PreferencesRepository
import com.example.backgroundapp.service.MonitoringForegroundService
import com.example.backgroundapp.ui.MonitoringScreen
import com.example.backgroundapp.ui.theme.BackgroundAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BackgroundApp
        val repo = app.preferencesRepository

        setContent {
            BackgroundAppTheme {
                val scope = rememberCoroutineScope()
                val settings by repo.settings.collectAsStateWithLifecycle(
                    initialValue = MonitorSettings("", "", false),
                )

                val notifLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) {
                    beginMonitoring(repo, scope)
                }

                val micLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            beginMonitoring(repo, scope)
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.toast_mic_required,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }

                fun tryStartMonitoring() {
                    if (settings.destinationEmail.isBlank()) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.toast_email_required,
                            Toast.LENGTH_LONG,
                        ).show()
                        return
                    }
                    if (settings.uploadEndpoint.isBlank()) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.toast_endpoint_recommended,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    when {
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO,
                        ) != PackageManager.PERMISSION_GRANTED -> {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED -> {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        else -> beginMonitoring(repo, scope)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MonitoringScreen(
                        onRequestMicPermission = {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onStartMonitoring = { tryStartMonitoring() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }
            }
        }
    }

    private fun beginMonitoring(repo: PreferencesRepository, scope: CoroutineScope) {
        scope.launch {
            repo.setMonitoringActive(true)
        }
        MonitoringForegroundService.start(this)
    }
}
