package com.aidbud.ai.vosk

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utility object responsible for extracting and decoding audio from video files.
 */
object AudioExtractor {
    private const val TAG = "AudioExtractor"
    private const val TIMEOUT_US = 10000

    /**
     * Extracts the audio track from a video file and decodes it to raw PCM data.
     *
     * @param context The application context.
     * @param videoUri The URI of the video file.
     * @return An [AudioData] object containing the PCM data, sample rate, and channel count,
     * or null if no audio track is found or extraction fails.
     * @throws IOException if an I/O error occurs during extraction or decoding.
     */
    @Throws(IOException::class)
    fun extractAndDecodeAudio(context: Context, videoUri: Uri): AudioData? {
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        val pcmOutputStream = ByteArrayOutputStream()

        try {
            extractor = MediaExtractor().apply { setDataSource(context, videoUri, null) }

            val audioTrackIndex = findAudioTrack(extractor)
            if (audioTrackIndex == -1) {
                Log.w(TAG, "No audio track found in video: $videoUri")
                return null
            }

            extractor.selectTrack(audioTrackIndex)
            val audioFormat = extractor.getTrackFormat(audioTrackIndex)

            val sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IOException("Audio MIME type not found.")

            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(audioFormat, null, null, 0)
                start()
            }

            val inputBuffers = codec.inputBuffers
            val outputBuffers = codec.outputBuffers
            val bufferInfo = MediaCodec.BufferInfo()
            var isInputEOS = false

            while (true) {
                if (!isInputEOS) {
                    val inputBufIndex = codec.dequeueInputBuffer(TIMEOUT_US.toLong())
                    if (inputBufIndex >= 0) {
                        val inputBuffer = inputBuffers[inputBufIndex]
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputBufIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isInputEOS = true
                        } else {
                            codec.queueInputBuffer(
                                inputBufIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0
                            )
                            extractor.advance()
                        }
                    }
                }

                val outputBufIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US.toLong())
                if (outputBufIndex >= 0) {
                    val outputBuffer = outputBuffers[outputBufIndex]
                    val pcmChunk = ByteArray(bufferInfo.size)
                    outputBuffer.get(pcmChunk)
                    // The outputBuffer position is advanced by outputBuffer.get,
                    // so clear and reset for next use.
                    outputBuffer.clear()
                    pcmOutputStream.write(pcmChunk)
                    codec.releaseOutputBuffer(outputBufIndex, false)
                }

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
            return AudioData(pcmOutputStream.toByteArray(), sampleRate, channels)
        } finally {
            extractor?.release()
            codec?.apply {
                stop()
                release()
            }
            pcmOutputStream.close()
        }
    }

    /**
     * Finds the audio track index within the given [MediaExtractor].
     * @param extractor The [MediaExtractor] instance.
     * @return The index of the audio track, or -1 if not found.
     */
    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }
}
