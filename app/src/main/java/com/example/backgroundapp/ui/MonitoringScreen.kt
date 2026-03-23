package com.example.backgroundapp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.backgroundapp.BackgroundApp
import com.example.backgroundapp.R
import com.example.backgroundapp.data.MonitorSettings
import com.example.backgroundapp.service.MonitoringForegroundService
import com.example.backgroundapp.upload.UploadWorkScheduler
import kotlinx.coroutines.launch

@Composable
fun MonitoringScreen(
    onRequestMicPermission: () -> Unit,
    onStartMonitoring: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as BackgroundApp
    val repo = app.preferencesRepository
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(
        initialValue = MonitorSettings("", "", false),
    )
    var emailDraft by remember(settings.destinationEmail) { mutableStateOf(settings.destinationEmail) }
    var endpointDraft by remember(settings.uploadEndpoint) { mutableStateOf(settings.uploadEndpoint) }

    LaunchedEffect(Unit) {
        UploadWorkScheduler.schedulePendingScan(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.screen_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.screen_intro), style = MaterialTheme.typography.bodyMedium)

        Button(
            onClick = onRequestMicPermission,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_request_mic))
        }

        OutlinedTextField(
            value = emailDraft,
            onValueChange = { emailDraft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_destination_email)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = endpointDraft,
            onValueChange = { endpointDraft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_upload_endpoint)) },
            placeholder = { Text(stringResource(R.string.hint_upload_endpoint)) },
            singleLine = true,
        )
        Button(
            onClick = {
                scope.launch {
                    repo.setDestinationEmail(emailDraft)
                    repo.setUploadEndpoint(endpointDraft)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_save_settings))
        }

        Spacer(Modifier.height(8.dp))

        Text(stringResource(R.string.section_battery), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.section_battery_body), style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = { openBatteryOptimizationSettings(context) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_battery_settings))
        }

        Spacer(Modifier.height(8.dp))

        Text(stringResource(R.string.section_monitor), style = MaterialTheme.typography.titleMedium)
        if (settings.monitoringActive) {
            Text(stringResource(R.string.status_monitoring_on), color = MaterialTheme.colorScheme.primary)
            Button(
                onClick = {
                    MonitoringForegroundService.stop(context)
                    scope.launch { repo.setMonitoringActive(false) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_stop_monitoring))
            }
        } else {
            Text(stringResource(R.string.status_monitoring_off))
            Button(
                onClick = onStartMonitoring,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_start_monitoring))
            }
            Text(
                stringResource(R.string.hint_start_requires_permissions),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun openBatteryOptimizationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, R.string.toast_battery_intent_failed, Toast.LENGTH_LONG).show()
    }
}
