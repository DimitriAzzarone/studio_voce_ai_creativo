package com.dimitriazzarone.studiovoceai.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.sqrt

sealed interface WaveformResult {
    data class Success(val amplitudes: List<Float>) : WaveformResult
    data class Unavailable(val message: String) : WaveformResult
}

class WaveformExtractor(context: Context) {
    private val appContext = context.applicationContext
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun extract(
        uri: Uri,
        barCount: Int = 180,
        callback: (WaveformResult) -> Unit
    ): Future<*> = executor.submit {
        val result = try {
            decode(uri, barCount.coerceIn(120, 300))
        } catch (_: SecurityException) {
            WaveformResult.Unavailable("Accesso al file non disponibile. Seleziona nuovamente l'audio.")
        } catch (_: IOException) {
            WaveformResult.Unavailable("Waveform non disponibile: il file non può essere letto.")
        } catch (_: IllegalArgumentException) {
            WaveformResult.Unavailable("Waveform non disponibile per questo formato.")
        } catch (_: RuntimeException) {
            WaveformResult.Unavailable("Waveform non disponibile per questo formato.")
        }

        if (!Thread.currentThread().isInterrupted) {
            mainHandler.post { callback(result) }
        }
    }

    private fun decode(uri: Uri, barCount: Int): WaveformResult {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(appContext, uri, null)

            var audioTrack = -1
            var inputFormat: MediaFormat? = null
            for (index in 0 until extractor.trackCount) {
                val candidate = extractor.getTrackFormat(index)
                val mime = candidate.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrack = index
                    inputFormat = candidate
                    break
                }
            }
            if (audioTrack < 0 || inputFormat == null) {
                return WaveformResult.Unavailable("Il file selezionato non contiene una traccia audio decodificabile.")
            }

            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: return WaveformResult.Unavailable("Formato audio non riconosciuto.")
            val durationUs = inputFormat.getLongOrNull(MediaFormat.KEY_DURATION)
            if (durationUs == null || durationUs <= 0L) {
                return WaveformResult.Unavailable("Waveform non disponibile: durata audio sconosciuta.")
            }

            extractor.selectTrack(audioTrack)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val squaredSums = DoubleArray(barCount)
            val sampleCounts = LongArray(barCount)
            val bufferInfo = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            while (!outputEnded && !Thread.currentThread().isInterrupted) {
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputEnded = true
                            } else {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime.coerceAtLeast(0L),
                                    0
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = codec.outputFormat
                        pcmEncoding = if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        } else {
                            AudioFormat.ENCODING_PCM_16BIT
                        }
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            val duplicate = outputBuffer.duplicate().order(ByteOrder.nativeOrder())
                            duplicate.position(bufferInfo.offset)
                            duplicate.limit(bufferInfo.offset + bufferInfo.size)
                            val bucket = ((bufferInfo.presentationTimeUs.toDouble() / durationUs) * barCount)
                                .toInt()
                                .coerceIn(0, barCount - 1)
                            accumulatePcm(duplicate.slice().order(ByteOrder.nativeOrder()), pcmEncoding, squaredSums, sampleCounts, bucket)
                        }
                        outputEnded = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }

            if (Thread.currentThread().isInterrupted) {
                return WaveformResult.Unavailable("Analisi waveform interrotta.")
            }

            val rms = FloatArray(barCount) { index ->
                if (sampleCounts[index] == 0L) 0f
                else sqrt(squaredSums[index] / sampleCounts[index]).toFloat()
            }
            val max = rms.maxOrNull()?.takeIf { it > 0f }
                ?: return WaveformResult.Unavailable("Waveform non disponibile: nessun campione PCM utile.")
            return WaveformResult.Success(rms.map { (it / max).coerceIn(0f, 1f) })
        } finally {
            try {
                codec?.stop()
            } catch (_: RuntimeException) {
            }
            try {
                codec?.release()
            } catch (_: RuntimeException) {
            }
            extractor.release()
        }
    }

    private fun accumulatePcm(
        buffer: ByteBuffer,
        encoding: Int,
        squaredSums: DoubleArray,
        sampleCounts: LongArray,
        bucket: Int
    ) {
        when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> {
                while (buffer.remaining() >= 4) {
                    val value = buffer.float.coerceIn(-1f, 1f).toDouble()
                    squaredSums[bucket] += value * value
                    sampleCounts[bucket]++
                }
            }
            AudioFormat.ENCODING_PCM_8BIT -> {
                while (buffer.hasRemaining()) {
                    val value = ((buffer.get().toInt() and 0xFF) - 128) / 128.0
                    squaredSums[bucket] += value * value
                    sampleCounts[bucket]++
                }
            }
            else -> {
                while (buffer.remaining() >= 2) {
                    val value = buffer.short / 32768.0
                    squaredSums[bucket] += value * value
                    sampleCounts[bucket]++
                }
            }
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }
}

private fun MediaFormat.getLongOrNull(key: String): Long? =
    if (containsKey(key)) getLong(key) else null
