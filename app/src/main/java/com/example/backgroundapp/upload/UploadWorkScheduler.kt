package com.example.backgroundapp.upload

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object UploadWorkScheduler {

    fun enqueueClipUpload(
        context: Context,
        filePath: String,
        recipientEmail: String,
        uploadEndpoint: String,
        deviceId: String = "",
    ) {
        if (uploadEndpoint.isBlank()) return
        val workName = "upload-${filePath.hashCode()}"
        val input = Data.Builder()
            .putString(AudioUploadWorker.KEY_FILE_PATH, filePath)
            .putString(AudioUploadWorker.KEY_EMAIL, recipientEmail)
            .putString(AudioUploadWorker.KEY_ENDPOINT, uploadEndpoint)
            .apply {
                if (deviceId.isNotBlank()) putString(AudioUploadWorker.KEY_DEVICE_ID, deviceId)
            }
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<AudioUploadWorker>()
            .setInputData(input)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30_000L,
                TimeUnit.MILLISECONDS,
            )
            .build()
        // REPLACE so a failed upload (e.g. 4xx after URL fix) or PendingScan can run again;
        // KEEP left permanent failures stuck with no retry for the same file path.
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /** Re-queue orphaned clips after restart (requires network for upload). */
    fun schedulePendingScan(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<PendingScanWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "pending-audio-scan",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
