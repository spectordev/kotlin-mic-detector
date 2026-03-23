package com.example.backgroundapp.upload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.backgroundapp.BackgroundApp
import com.example.backgroundapp.service.MonitoringForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Scans the recordings directory and re-queues uploads for orphaned `.wav` files
 * (e.g. after process death). Idempotent via [UploadWorkScheduler.enqueueClipUpload].
 */
class PendingScanWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as? BackgroundApp ?: return@withContext Result.success()
        val repo = app.preferencesRepository
        val settings = repo.settings.first()
        if (settings.uploadEndpoint.isBlank() || settings.destinationEmail.isBlank()) {
            return@withContext Result.success()
        }
        val dir = File(applicationContext.filesDir, MonitoringForegroundService.RECORDINGS_SUBDIR)
        if (!dir.isDirectory) return@withContext Result.success()
        dir.listFiles { f -> f.isFile && f.name.endsWith(".wav") }?.forEach { file ->
            UploadWorkScheduler.enqueueClipUpload(
                applicationContext,
                file.absolutePath,
                settings.destinationEmail,
                settings.uploadEndpoint,
            )
        }
        Result.success()
    }
}
