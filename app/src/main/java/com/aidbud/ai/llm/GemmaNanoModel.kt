package com.aidbud.ai.llm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import com.google.ai.edge.localagents.rag.models.Embedder
import com.google.ai.edge.localagents.rag.models.GeckoEmbeddingModel
import java.util.Optional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume

val MAX_IMAGES = 64
var MAX_TOKENS = 2048
var FRAMES_PER_SECOND = 1


class ModelLoadFailException :
    Exception("Failed to load model, please try again")

class ModelSessionCreateFailException :
    Exception("Failed to create model session, please try again")

class GemmaNanoModel(
    private val context: Context
) {
    private var llmInference: LlmInference? = null
    private var llmInferenceSession: LlmInferenceSession? = null
    private var inferenceOptions = LlmInference.LlmInferenceOptions.builder()
        .setModelPath("${context.filesDir}/gemma3n4b-aidbud.task")
        .setMaxTokens(MAX_TOKENS)
        .setMaxNumImages(MAX_IMAGES)
        .setPreferredBackend(LlmInference.Backend.CPU)
        .build()
    private val sessionOptions =  LlmInferenceSessionOptions.builder()
        .setTemperature(1.0f)
        .setTopK(64)
        .setTopP(0.95f)
        .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
        .build()

    companion object {
        private const val TAG = "GemmaNanoModel"
        // A configurable constant for the number of frames to extract from a video.
        private const val FRAMES_PER_VIDEO = 1
    }

    private fun createEngine() {
        try {
            llmInference = LlmInference.createFromOptions(context, inferenceOptions)
        } catch (e: Exception) {
            Log.e(TAG, "Load model error: ${e.message}", e)
            throw ModelLoadFailException()
        }
    }

    private fun createSession() {
        if (llmInference == null) {
            createEngine()
        }
        try {
            llmInferenceSession = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
        } catch (e: Exception) {
            Log.e(TAG, "LlmInferenceSession create error: ${e.message}", e)
            throw ModelSessionCreateFailException()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun generateResponse(
        prompt: String,
        attachments: List<Uri> = emptyList()
    ): Flow<String> {

        return callbackFlow {
            val response = StringBuilder()
            val job = launch(Dispatchers.IO) {
                // Define session options, enabling the vision modality.
                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
                    .setTemperature(1.0f)
                    .setTopK(64)
                    .setTopP(0.95f)
                    .build()

                // Use a try-with-resources block to ensure the session is closed automatically.
                try {
                    llmInferenceSession =
                        LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
                    llmInferenceSession!!.addQueryChunk(prompt)
                    val bitmaps = processUris(attachments)
                    bitmaps.forEach { bitmap ->
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        llmInferenceSession!!.addImage(mpImage)
                    }

                    val progressListener = ProgressListener<String> { partialResult, done ->
                        // Check if the flow is still active before processing results
                        if (!isClosedForSend) {
                            response.append(partialResult)
                            if (done) {
                                // When done, emit the complete string and close the flow
                                trySend(response.toString())
                                close() // Signal that the flow has completed successfully
                            }
                        } else {
                            // If the flow is closed (e.g., cancelled by consumer),
                            // attempt to cancel the underlying LLM operation.
                            Log.d(TAG, "Flow cancelled, attempting to stop LLM generation.")
                            llmInferenceSession?.cancelGenerateResponseAsync()
                        }
                    }

                    // 5. Start the asynchronous generation process on the session.
                    llmInferenceSession!!.generateResponseAsync(progressListener)

                } catch (e: Exception) {
                    Log.e(TAG, "Error during session creation or generation", e)
                    close(e)
                }
            }

            awaitClose {
                Log.d(TAG, "Flow awaitClose triggered. Cancelling LLM job and closing session.")
                job.cancel() // Cancel the coroutine job that started the LLM operation
                try {
                    llmInferenceSession?.cancelGenerateResponseAsync() // Attempt to cancel the LLM operation
                    llmInferenceSession?.close() // Close the LLM session
                    llmInferenceSession = null
                } catch (closeError: Exception) {
                    Log.w(TAG, "Error while closing LLM session during awaitClose", closeError)
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun generateResponsePCard(
        prompt: String,
        attachments: List<Uri> = emptyList()
    ): Flow<ModelResponse> {
        return callbackFlow {
            val parser = ModelResponseParser { modelResponse ->
                // The parser will call this lambda for each complete response part
                if (!isClosedForSend) {
                    trySend(modelResponse)
                }
            }

            val job = launch(Dispatchers.IO) {
                // Define session options, enabling the vision modality.
                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
                    .setTemperature(1.0f)
                    .setTopK(64)
                    .setTopP(0.95f)
                    .build()

                // Use a try-with-resources block to ensure the session is closed automatically.
                try {
                    llmInferenceSession =
                        LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
                    llmInferenceSession!!.addQueryChunk(prompt)
                    val bitmaps = processUris(attachments)
                    bitmaps.forEach { bitmap ->
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        llmInferenceSession!!.addImage(mpImage)
                    }


                    val listener = ProgressListener<String> { partialResult, done ->
                        // Check if the flow is still active before processing results
                        if (!isClosedForSend) {
                            // Feed the token to the parser
                            parser.processToken(partialResult)

                            if (done) {
                                // Flush any remaining content and close the flow
                                parser.complete()
                                parser.reset()
                                close() // Signal that the flow has completed successfully
                            }
                        } else {
                            // If the flow is closed (e.g., cancelled by consumer),
                            // attempt to cancel the underlying LLM operation.
                            Log.d(TAG, "Flow cancelled, attempting to stop LLM generation.")
                            llmInferenceSession?.cancelGenerateResponseAsync()
                        }
                    }

                    // 5. Start the asynchronous generation process on the session.
                    llmInferenceSession!!.generateResponseAsync(listener)

                } catch (e: Exception) {
                    Log.e(TAG, "Error during session creation or generation", e)
                    close(e) // Close the flow with the exception
                }
                // Removed the 'finally' block as cleanup is handled in awaitClose
            }

            awaitClose {
                Log.d(TAG, "Flow awaitClose triggered. Cancelling LLM job and closing session.")
                job.cancel() // Cancel the coroutine job that started the LLM operation
                try {
                    llmInferenceSession?.cancelGenerateResponseAsync() // Attempt to cancel the LLM operation
                    llmInferenceSession?.close() // Close the LLM session
                    llmInferenceSession = null // Nullify the session reference
                } catch (closeError: Exception) {
                    Log.w(TAG, "Error while closing LLM session during awaitClose", closeError)
                }
            }
        }
    }

    fun stopResponse() {
        llmInferenceSession!!.cancelGenerateResponseAsync()
    }

    private suspend fun processUris(uris: List<Uri>): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        val contentResolver = context.contentResolver

        for (uri in uris) {
            try {
                when (val mimeType = contentResolver.getType(uri)) {
                    null -> Log.w(TAG, "Could not determine MIME type for URI: $uri")
                    in MimeType.IMAGE_TYPES -> {
                        val source = ImageDecoder.createSource(contentResolver, uri)
                        val bitmap = ImageDecoder.decodeBitmap(source)
                        bitmaps.add(bitmap.copy(Bitmap.Config.ARGB_8888, false))
                    }

                    in MimeType.VIDEO_TYPES -> {
                        bitmaps.addAll(extractFramesFromVideo(uri))
                    }

                    else -> Log.w(TAG, "Unsupported MIME type: $mimeType for URI: $uri")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process URI: $uri", e)
            }
        }
        bitmaps
    }

    private suspend fun extractFramesFromVideo(uri: Uri): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0

            if (durationMs > 0) {
                val frameIntervalUs = 1_000_000 / FRAMES_PER_SECOND
                val totalFrames = (durationMs * 1000) / frameIntervalUs
                for (i in 0 until totalFrames) {
                    val timeUs = i * frameIntervalUs
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { frame ->
                        frames.add(frame)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting frames from video: $uri", e)
        } finally {
            retriever.release()
        }
        frames
    }

    /**
     * Releases resources used by the LlmInference engine.
     */
    fun closeSession() {
        llmInferenceSession?.close()
        llmInferenceSession = null
    }

    fun close() {
        llmInference?.close()
        llmInference = null
        closeSession()
    }
}

/**
 * A helper object to contain common MIME types for easy checking.
 */
object MimeType {
    val IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp")
    val VIDEO_TYPES = setOf("video/mp4", "video/3gpp", "video/webm", "video/avi")
}
