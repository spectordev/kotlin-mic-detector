package com.example.backgroundapp.audio

object VoiceActivityConfig {
    const val SAMPLE_RATE = 16_000
    /** ~50 ms frames at 16 kHz */
    const val FRAME_SAMPLES = 800
    /** RMS threshold on 16-bit samples (tune on device). */
    const val RMS_THRESHOLD = 1200.0
    /** Loud frames needed before starting a clip (~400 ms at 50 ms/frame). */
    const val SPEECH_START_FRAMES = 8
    /** Silent frames before ending clip (~3 s). */
    const val SILENCE_END_FRAMES = 60
    /** Hard cap per clip to avoid huge files (~8 MiB PCM). */
    const val MAX_PCM_BYTES: Long = 8L * 1024L * 1024L
}

fun rmsOfShorts(buffer: ShortArray, length: Int): Double {
    if (length <= 0) return 0.0
    var sum = 0.0
    for (i in 0 until length) {
        val s = buffer[i].toDouble()
        sum += s * s
    }
    return kotlin.math.sqrt(sum / length)
}
