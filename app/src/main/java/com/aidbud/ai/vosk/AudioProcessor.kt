package com.aidbud.ai.vosk

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Utility object responsible for processing raw audio data,
 * including resampling and channel conversion.
 */
object AudioProcessor {

    /**
     * Resamples the PCM data to the target sample rate (16kHz) and converts it to mono if necessary.
     *
     * @param pcmData The raw PCM audio data.
     * @param originalRate The original sample rate of the PCM data.
     * @param targetRate The desired target sample rate (e.g., 16000 for Vosk).
     * @param channels The number of channels in the original PCM data (1 for mono, 2 for stereo).
     * @return The processed PCM data as a byte array, resampled and converted to mono.
     */
    fun resamplePcm(
        pcmData: ByteArray,
        originalRate: Int,
        targetRate: Int,
        channels: Int
    ): ByteArray {
        // Convert byte array to short array (16-bit PCM)
        val originalBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val originalShorts = ShortArray(originalBuffer.remaining())
        originalBuffer.get(originalShorts)

        // Handle stereo to mono conversion by averaging channels
        val monoShorts: ShortArray = if (channels == 2) {
            ShortArray(originalShorts.size / 2) { i ->
                ((originalShorts[i * 2] + originalShorts[i * 2 + 1]) / 2).toShort()
            }
        } else {
            originalShorts
        }

        if (originalRate == targetRate) {
            // No resampling needed, just convert back to byte array if only channel conversion occurred
            val byteBuffer = ByteBuffer.allocate(monoShorts.size * 2)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(monoShorts)
            return byteBuffer.array()
        }

        // Perform resampling using linear interpolation
        val resampledLength = (monoShorts.size * (targetRate.toDouble() / originalRate)).roundToInt()
        val resampledShorts = ShortArray(resampledLength)
        val step = (monoShorts.size - 1).toDouble() / (resampledLength - 1).coerceAtLeast(1) // Avoid division by zero

        for (i in 0 until resampledLength) {
            val originalIndex = i * step
            val index1 = originalIndex.toInt()
            val index2 = min(index1 + 1, monoShorts.size - 1)
            val fraction = originalIndex - index1
            resampledShorts[i] =
                (monoShorts[index1] * (1 - fraction) + monoShorts[index2] * fraction).roundToInt().toShort()
        }

        // Convert resampled short array back to byte array
        val byteBuffer = ByteBuffer.allocate(resampledShorts.size * 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(resampledShorts)
        return byteBuffer.array()
    }
}
