package com.example.backgroundapp.network

import android.content.Context
import android.provider.Settings
import com.example.backgroundapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object BackendDeviceSync {

    suspend fun registerDeviceEmail(context: Context, destinationEmail: String) {
        val base = BuildConfig.BACKEND_BASE_URL.trim().trimEnd('/')
        if (base.isEmpty() || destinationEmail.isBlank()) return
        val deviceId =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: return
        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val json = JSONObject()
                .put("device_id", deviceId)
                .put("email", destinationEmail.trim())
                .toString()
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$base/device/email")
                .post(body)
                .build()
            client.newCall(request).execute().use { /* ignore body */ }
        }
    }
}
