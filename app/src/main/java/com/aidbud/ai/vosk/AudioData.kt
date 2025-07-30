package com.aidbud.ai.vosk

/**
 * A data class to hold extracted audio information.
 *
 * @property pcmData The raw PCM audio data as a byte array.
 * @property sampleRate The sample rate of the audio data in Hz.
 * @property channels The number of audio channels (e.g., 1 for mono, 2 for stereo).
 */
data class AudioData(val pcmData: ByteArray, val sampleRate: Int, val channels: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioData

        if (!pcmData.contentEquals(other.pcmData)) return false
        if (sampleRate != other.sampleRate) return false
        if (channels != other.channels) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pcmData.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channels
        return result
    }
}