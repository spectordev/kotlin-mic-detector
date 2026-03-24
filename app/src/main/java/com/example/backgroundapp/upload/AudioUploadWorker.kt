package com.example.backgroundapp.upload

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Uploads a finalized clip to your HTTPS backend. Expected contract (adjust server-side):
 *
 * POST [uploadEndpoint] — multipart form:
 * - `recipient_email`: destination address
 * - `audio`: WAV file
 *
 * Backend should attach the file and send email (or store); do not embed SMTP credentials in the app.
 */
class AudioUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val path = inputData.getString(KEY_FILE_PATH) ?: return@withContext Result.failure()
        val email = inputData.getString(KEY_EMAIL).orEmpty()
        val endpoint = inputData.getString(KEY_ENDPOINT).orEmpty()
        val deviceId = inputData.getString(KEY_DEVICE_ID).orEmpty()
        if (email.isBlank() || endpoint.isBlank()) {
            return@withContext Result.failure()
        }
        val httpUrl = endpoint.trim().toHttpUrlOrNull()
        if (httpUrl == null) {
            Log.w(TAG, "Invalid upload URL (malformed): ${endpoint.take(80)}")
            return@withContext Result.failure()
        }
        val file = File(path)
        if (!file.exists() || !file.canRead()) {
            return@withContext Result.success()
        }
        if (file.length() > MAX_UPLOAD_BYTES) {
            Log.w(TAG, "Clip too large, deleting: ${file.length()}")
            file.delete()
            return@withContext Result.success()
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("recipient_email", email)
            .apply {
                if (deviceId.isNotBlank()) {
                    addFormDataPart("device_id", deviceId)
                }
            }
            .addFormDataPart(
                "audio",
                file.name,
                file.asRequestBody("audio/wav".toMediaType()),
            )
            .build()
        val request = Request.Builder()
            .url(httpUrl)
            .post(body)
            .build()
        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    if (!file.delete()) {
                        Log.w(TAG, "Upload ok but failed to delete ${file.absolutePath}")
                    }
                    Result.success()
                } else {
                    val code = response.code
                    val retryable = code >= 500 || code == 408 || code == 429
                    Log.w(TAG, "Upload failed: $code ${response.message}")
                    if (retryable) Result.retry() else Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Upload error", e)
            Result.retry()
        }
    }

    companion object {
        const val KEY_FILE_PATH = "file_path"
        const val KEY_EMAIL = "recipient_email"
        const val KEY_ENDPOINT = "upload_endpoint"
        const val KEY_DEVICE_ID = "device_id"
        private const val TAG = "AudioUploadWorker"
        private const val MAX_UPLOAD_BYTES = 12L * 1024L * 1024L
    }
}
