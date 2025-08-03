package com.aidbud.ai.vosk

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.IOException
import java.lang.IllegalStateException

/**
 * A utility class for transcribing audio from video files using the Vosk speech recognition engine.
 * This class handles Vosk model initialization internally and provides a suspend function for transcription,
 * making the process asynchronous and simpler to manage with Kotlin Coroutines.
 *
 * @param context The application context. This should typically be the Application context
 * to prevent potential memory leaks related to long-running model unpacking.
 * @param modelPath The path within the assets folder where the Vosk model is located.
 */
class VideoTranscriber(private val context: Context, private val modelPath: String) {

    private var voskModel: Model? = null
    private val modelDeferred = CompletableDeferred<Model>()
    private val initMutex = Mutex() // To prevent multiple simultaneous initializations

    init {
        // Start model initialization as soon as the transcriber is created
        initializeVoskModel()
    }

    /**
     * Initializes the Vosk model from the app's assets.
     * This is called automatically when a VideoTranscriber instance is created.
     */
    private fun initializeVoskModel() {
        if (voskModel != null || modelDeferred.isCompleted) {
            Log.d(TAG, "Vosk model already initialized or initialization in progress.")
            return
        }

        StorageService.unpack(
            context, modelPath, "model",
            { unpackedModel: Model? ->
                if (unpackedModel != null) {
                    voskModel = unpackedModel
                    modelDeferred.complete(unpackedModel)
                    Log.d(TAG, "Vosk model initialized successfully.")
                } else {
                    val error = IOException("Unpacked model was null.")
                    modelDeferred.completeExceptionally(error)
                    Log.e(TAG, "Failed to initialize Vosk model: unpacked model is null.", error)
                }
            },
            { exception: IOException? ->
                val error = exception ?: IOException("Unknown error during Vosk model unpacking.")
                modelDeferred.completeExceptionally(error)
                Log.e(TAG, "Failed to initialize Vosk model: ${error.message}", error)
            }
        )
    }

    /**
     * Transcribes the audio from a given video URI.
     * This is a suspend function, meaning it can be called from a coroutine and will
     * pause execution until the transcription is complete or an error occurs.
     *
     * @param videoUri The URI of the video file to transcribe.
     * @return The transcribed text as a [String?], or null if no speech was detected.
     * @throws IOException if audio extraction or decoding fails.
     * @throws IllegalStateException if the Vosk model is not available or an internal Vosk error occurs.
     * @throws Exception for any other unexpected errors during the process.
     */
    suspend fun transcribeVideo(videoUri: Uri): String? = withContext(Dispatchers.IO) {
        val model = try {
            modelDeferred.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Vosk model for transcription: ${e.message}", e)
            throw IllegalStateException("Vosk model failed to initialize.", e)
        }

        val audioData = AudioExtractor.extractAndDecodeAudio(context, videoUri)
            ?: throw IOException("Failed to extract audio from video. No audio track found or extraction failed.")

        val resampledData = AudioProcessor.resamplePcm(
            audioData.pcmData,
            audioData.sampleRate,
            TARGET_SAMPLE_RATE,
            audioData.channels
        )

        performTranscription(resampledData, model)
    }

    /**
     * Feeds the processed audio data into the Vosk Recognizer and obtains the transcription.
     *
     * @param pcmData The resampled and mono PCM audio data.
     * @param model The initialized Vosk Model.
     * @return The transcribed text, or null if no speech was detected.
     * @throws Exception if an error occurs during Vosk recognition.
     */
    private fun performTranscription(pcmData: ByteArray, model: Model): String? {
        val recognizer = Recognizer(model, TARGET_SAMPLE_RATE.toFloat()).apply {
            // setWords(true)
        }

        try {
            recognizer.acceptWaveForm(pcmData, pcmData.size)
            return recognizer.finalResult
        } catch (e: Exception) {
            Log.e(TAG, "Error during Vosk recognition: ${e.message}", e)
            throw e // Re-throw the exception for the suspend function to catch
        } finally {
            recognizer.close()
        }
    }

    /**
     * Releases the Vosk model resources. Call this when the transcriber is no longer needed
     * (e.g., in onCleared of a ViewModel, or onDestroy of an Activity/Fragment if tied to its lifecycle).
     */
    fun release() {
        voskModel?.close()
        voskModel = null
        Log.d(TAG, "Vosk model released.")
    }

    companion object {
        private const val TAG = "VideoTranscriber"
        private const val TARGET_SAMPLE_RATE = 16000 // Vosk requires 16kHz
    }
}