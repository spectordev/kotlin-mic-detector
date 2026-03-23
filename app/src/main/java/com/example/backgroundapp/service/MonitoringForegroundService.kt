package com.example.backgroundapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.backgroundapp.BackgroundApp
import com.example.backgroundapp.MainActivity
import com.example.backgroundapp.R
import com.example.backgroundapp.audio.VoiceActivityConfig
import com.example.backgroundapp.audio.WavSegmentWriter
import com.example.backgroundapp.audio.rmsOfShorts
import com.example.backgroundapp.data.PreferencesRepository
import com.example.backgroundapp.upload.UploadWorkScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MonitoringForegroundService : Service() {

    private val running = AtomicBoolean(false)
    @Volatile
    private var audioThread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val stopExecutor = Executors.newSingleThreadExecutor()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            requestStopGracefully()
            return START_NOT_STICKY
        }
        if (!hasRecordPermission()) {
            Log.w(TAG, "RECORD_AUDIO missing; stopping service")
            requestStopGracefully()
            return START_NOT_STICKY
        }
        val notification = buildNotification(getString(R.string.notification_monitoring_active))
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        synchronized(this) {
            if (audioThread?.isAlive != true) {
                running.set(true)
                audioThread = Thread({ runAudioLoop() }, "monitor-audio").also { it.start() }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running.set(false)
        audioThread?.interrupt()
        stopExecutor.execute {
            try {
                audioThread?.join(3000)
            } catch (_: InterruptedException) {
            }
        }
        stopExecutor.shutdown()
        super.onDestroy()
    }

    /**
     * Stops capture from another thread; does not block the calling thread for long.
     * Avoid calling from the audio thread (it exits on its own when [running] is false).
     */
    private fun requestStopGracefully() {
        running.set(false)
        audioThread?.interrupt()
        stopExecutor.execute {
            try {
                audioThread?.join(8000)
            } catch (_: InterruptedException) {
            }
        }
    }

    private fun runAudioLoop() {
        val repo = preferencesRepository()
        val settings = runBlocking { repo.settings.first() }
        val email = settings.destinationEmail
        val endpoint = settings.uploadEndpoint
        val deviceId =
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val recordingsDir = File(filesDir, RECORDINGS_SUBDIR).apply { mkdirs() }
        val readBuffer = ShortArray(VoiceActivityConfig.FRAME_SAMPLES)
        var recording = false
        var loudStreak = 0
        var silentStreak = 0
        var writer: WavSegmentWriter? = null
        var currentFile: File? = null
        var record: AudioRecord? = null
        try {
            record = try {
                openAudioRecord()
            } catch (e: Exception) {
                Log.e(TAG, "Microphone unavailable", e)
                updateNotification(getString(R.string.notification_mic_error))
                return
            }
            try {
                record.startRecording()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Could not start recording", e)
                return
            }
            while (running.get()) {
                if (!hasRecordPermission()) {
                    Log.w(TAG, "Permission revoked")
                    updateNotification(getString(R.string.notification_permission_revoked))
                    break
                }
                val r = record
                val read = try {
                    r.read(readBuffer, 0, readBuffer.size)
                } catch (e: Exception) {
                    Log.w(TAG, "read failed", e)
                    -1
                }
                if (read == AudioRecord.ERROR_INVALID_OPERATION ||
                    read == AudioRecord.ERROR_BAD_VALUE ||
                    read <= 0
                ) {
                    Log.w(TAG, "Audio read error: $read")
                    Thread.sleep(200)
                    continue
                }
                val rms = rmsOfShorts(readBuffer, read)
                val loud = rms >= VoiceActivityConfig.RMS_THRESHOLD
                if (!recording) {
                    if (loud) loudStreak++ else loudStreak = 0
                    if (loudStreak >= VoiceActivityConfig.SPEECH_START_FRAMES) {
                        recording = true
                        silentStreak = 0
                        currentFile = File(
                            recordingsDir,
                            "clip_${System.currentTimeMillis()}_${UUID.randomUUID()}.wav",
                        )
                        writer = WavSegmentWriter(currentFile!!, VoiceActivityConfig.SAMPLE_RATE)
                        writer.writePcm16(readBuffer, read)
                    }
                } else {
                    val w = writer ?: continue
                    w.writePcm16(readBuffer, read)
                    if (!loud) silentStreak++ else silentStreak = 0
                    val tooBig = w.currentPcmBytes() >= VoiceActivityConfig.MAX_PCM_BYTES
                    val silenceDone = silentStreak >= VoiceActivityConfig.SILENCE_END_FRAMES
                    if (silenceDone || tooBig) {
                        try {
                            w.close()
                        } catch (e: Exception) {
                            Log.w(TAG, "finalize wav failed", e)
                        }
                        writer = null
                        val file = currentFile
                        currentFile = null
                        recording = false
                        loudStreak = 0
                        silentStreak = 0
                        if (file != null && file.exists() && file.length() > MIN_CLIP_BYTES) {
                            if (endpoint.isNotBlank() && email.isNotBlank()) {
                                UploadWorkScheduler.enqueueClipUpload(
                                    this@MonitoringForegroundService,
                                    file.absolutePath,
                                    email,
                                    endpoint,
                                    deviceId,
                                )
                            }
                        } else {
                            file?.delete()
                        }
                    }
                }
            }
        } finally {
            try {
                writer?.close()
            } catch (_: Exception) {
            }
            record?.run {
                try {
                    stop()
                } catch (_: Exception) {
                }
                release()
            }
            runBlocking { repo.setMonitoringActive(false) }
            mainHandler.post {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun openAudioRecord(): AudioRecord {
        val minBuf = AudioRecord.getMinBufferSize(
            VoiceActivityConfig.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        require(minBuf > 0) { "Invalid buffer size $minBuf" }
        val frameBytes = VoiceActivityConfig.FRAME_SAMPLES * 2
        val bufferSize = minBuf.coerceAtLeast(frameBytes * 4)
        return AudioRecord(
            MediaRecorder.AudioSource.MIC,
            VoiceActivityConfig.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        ).also { ar ->
            if (ar.state != AudioRecord.STATE_INITIALIZED) {
                ar.release()
                throw IllegalStateException("AudioRecord not initialized")
            }
        }
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    private fun preferencesRepository(): PreferencesRepository =
        (application as BackgroundApp).preferencesRepository

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, MonitoringForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setContentIntent(open)
            .addAction(0, getString(R.string.notification_action_stop), stop)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = ContextCompat.getSystemService(this, NotificationManager::class.java)
        nm?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val RECORDINGS_SUBDIR = "recordings"
        private const val CHANNEL_ID = "monitoring"
        private const val NOTIFICATION_ID = 42
        private const val TAG = "MonitoringService"
        const val ACTION_STOP = "com.example.backgroundapp.ACTION_STOP_MONITORING"
        private const val MIN_CLIP_BYTES = 8_192L

        fun start(context: Context) {
            val intent = Intent(context, MonitoringForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val i = Intent(context, MonitoringForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(i)
        }
    }
}
