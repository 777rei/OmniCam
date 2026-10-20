package com.example.engine.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class AlarmSoundManager(private val context: Context) {
    private val TAG = "AlarmSoundManager"
    private var sirenJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    var isSirenPlaying = false
        private set

    /**
     * Plays a high-pitched security surveillance siren programmatically using AudioTrack.
     */
    fun startSiren(durationMs: Long = 4000) {
        if (isSirenPlaying) return
        isSirenPlaying = true

        sirenJob = scope.launch {
            val sampleRate = 44100
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize.coerceAtLeast(4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack.play()

            val startTime = System.currentTimeMillis()
            var phase = 0.0

            try {
                val bufferSize = 1024
                val buffer = ShortArray(bufferSize)

                while (isActive && isSirenPlaying && (System.currentTimeMillis() - startTime < durationMs)) {
                    val elapsed = System.currentTimeMillis() - startTime
                    // Sweep frequency up and down every 600ms between 700Hz and 1500Hz
                    val cycle = (elapsed % 600) / 600.0
                    val currentFreq = if (cycle < 0.5) {
                        700.0 + (cycle * 2.0) * 800.0
                    } else {
                        1500.0 - ((cycle - 0.5) * 2.0) * 800.0
                    }

                    val phaseIncrement = (2.0 * Math.PI * currentFreq) / sampleRate

                    for (i in 0 until bufferSize) {
                        val sample = (sin(phase) * 32767.0 * 0.85).toInt().toShort()
                        buffer[i] = sample
                        phase += phaseIncrement
                        if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                    }
                    audioTrack.write(buffer, 0, bufferSize)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Siren error: ${e.message}")
            } finally {
                runCatching {
                    audioTrack.stop()
                    audioTrack.release()
                }
                isSirenPlaying = false
            }
        }
    }

    fun stopSiren() {
        isSirenPlaying = false
        sirenJob?.cancel()
        sirenJob = null
    }
}
