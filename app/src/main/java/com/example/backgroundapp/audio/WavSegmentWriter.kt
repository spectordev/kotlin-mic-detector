package com.example.backgroundapp.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

/**
 * Writes 16-bit mono PCM and produces a valid WAV on [close].
 */
class WavSegmentWriter(
    private val file: File,
    private val sampleRate: Int,
) {
    private val raf = RandomAccessFile(file, "rw")
    private var pcmBytesWritten = 0L

    init {
        raf.setLength(0)
        raf.seek(HEADER_BYTES.toLong())
    }

    fun writePcm16(buffer: ShortArray, length: Int) {
        val bb = ByteArray(length * 2)
        var bi = 0
        for (i in 0 until length) {
            val s = buffer[i].toInt()
            bb[bi++] = (s and 0xFF).toByte()
            bb[bi++] = (s shr 8 and 0xFF).toByte()
        }
        raf.write(bb)
        pcmBytesWritten += bb.size
    }

    fun currentPcmBytes(): Long = pcmBytesWritten

    fun close() {
        try {
            writeHeader()
        } finally {
            raf.close()
        }
    }

    private fun writeHeader() {
        val totalDataLen = pcmBytesWritten
        val riffChunkSize = 36 + totalDataLen
        raf.seek(0)
        writeAscii("RIFF")
        writeLeInt(riffChunkSize.toInt())
        writeAscii("WAVE")
        writeAscii("fmt ")
        writeLeInt(16)
        writeLeShort(1)
        writeLeShort(1)
        writeLeInt(sampleRate)
        writeLeInt(sampleRate * 2)
        writeLeShort(2)
        writeLeShort(16)
        writeAscii("data")
        writeLeInt(totalDataLen.toInt())
    }

    private fun writeAscii(s: String) {
        raf.write(s.toByteArray(StandardCharsets.US_ASCII))
    }

    private fun writeLeInt(v: Int) {
        raf.write(v and 0xFF)
        raf.write(v shr 8 and 0xFF)
        raf.write(v shr 16 and 0xFF)
        raf.write(v shr 24 and 0xFF)
    }

    private fun writeLeShort(v: Int) {
        raf.write(v and 0xFF)
        raf.write(v shr 8 and 0xFF)
    }

    companion object {
        private const val HEADER_BYTES = 44
    }
}
