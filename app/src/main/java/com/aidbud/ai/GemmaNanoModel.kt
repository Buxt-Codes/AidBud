import MimeType
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.contains
import kotlin.math.max
import org.json.JSONObject

import com.aidbud.ai.ModelResponse
import com.aidbud.ai.ModelResponseParser

val MAX_IMAGES = 64
var MAX_TOKENS = 2048

var DECODE_TOKEN_OFFSET = 256
var MODEL_PATH = ""
var FRAMES_PER_SECOND = 1

class ModelLoadFailException :
    Exception("Failed to load model, please try again")

class ModelSessionCreateFailException :
    Exception("Failed to create model session, please try again")

/**
 * A service class to handle interactions with an on-device Large Language Model
 * using the MediaPipe LlmInference task. This version is updated to correctly
 * handle multimodal inputs (text, image, video) using LlmInferenceSession.
 *
 * @param context The application context, needed for accessing assets and services.
 */
class LlmInferenceService(private val context: Context) {

    private var llmInference: LlmInference? = null
    private var llmInferenceSession: LlmInferenceSession? = null

    companion object {
        private const val TAG = "LlmInferenceService"
        // A configurable constant for the number of frames to extract from a video.
        private const val FRAMES_PER_VIDEO = 1
    }

    private fun createEngine(context: Context) {
        val inferenceOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(MODEL_PATH)
            .setMaxTokens(MAX_TOKENS)
            .setMaxNumImages(MAX_IMAGES)
            .build()

        try {
            llmInference = LlmInference.createFromOptions(context, inferenceOptions)
        } catch (e: Exception) {
            Log.e(TAG, "Load model error: ${e.message}", e)
            throw ModelLoadFailException()
        }
    }

    private fun createSession() {

        val inferenceOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(MODEL_PATH)
            .setMaxTokens(MAX_TOKENS)
            .build()

        val sessionOptions =  LlmInferenceSessionOptions.builder()
            .setTemperature(1.0f)
            .setTopK(64)
            .setTopP(0.95f)
            .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
            .build()

        try {
            llmInferenceSession = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
        } catch (e: Exception) {
            Log.e(TAG, "LlmInferenceSession create error: ${e.message}", e)
            throw ModelSessionCreateFailException()
        }
    }

//    fun estimateTokensRemaining(prompt: String): Int {
//        val context = uiState.messages.joinToString { it.rawMessage } + prompt
//        if (context.isEmpty()) return -1 // Specia marker if no content has been added
//
//        val sizeOfAllMessages = llmInferenceSession.sizeInTokens(context)
//        val approximateControlTokens = uiState.messages.size * 3
//        val remainingTokens = MAX_TOKENS - sizeOfAllMessages - approximateControlTokens -  DECODE_TOKEN_OFFSET
//        // Token size is approximate so, let's not return anything below 0
//        return max(0, remainingTokens)
//    }
//
//    /**
//     * Generates a response from the LLM based on a text prompt and a list of media URIs.
//     * This method now uses LlmInferenceSession to properly handle multimodal inputs.
//     *
//     * @param prompt The primary text prompt.
//     * @param attachments A list of Uris pointing to images or videos.
//     * @return A Flow<String> that emits the generated text token-by-token.
//     * @throws IllegalStateException if the service has not been initialized.
//     */
    fun generateResponse(
        prompt: String,
        attachments: List<Uri> = emptyList()
    ): Flow<String> {

        return callbackFlow {
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
                        trySend(partialResult)
                        if (done) close()
                    }

                    // 5. Start the asynchronous generation process on the session.
                    llmInferenceSession!!.generateResponseAsync(progressListener)
                    llmInferenceSession!!.close()

                } catch (e: Exception) {
                    Log.e(TAG, "Error during session creation or generation", e)
                    close(e)
                } finally {
                    try {
                        llmInferenceSession!!.close()
                        llmInferenceSession = null
                    } catch (closeError: Exception) {
                        Log.w(TAG, "Error while closing session", closeError)
                    }
                }
            }

            awaitClose {
                job.cancel()
            }
        }
    }

    fun generateResponsePCard(
        prompt: String,
        attachments: List<Uri> = emptyList()
    ): Flow<ModelResponse> {
        val startToken = "[PCARD]"
        val endToken   = "[PCARD]"
        val buffer = StringBuilder()

        return callbackFlow {
            val parser = ModelResponseParser { modelResponse ->
                // The parser will call this lambda for each complete response part
                trySend(modelResponse)
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
                        // Feed the token to the parser
                        parser.processToken(partialResult)

                        if (done) {
                            // 3. Flush any remaining content and close the flow
                            parser.completed()
                            close()
                        }
                    }

                    // 5. Start the asynchronous generation process on the session.
                    llmInferenceSession!!.generateResponseAsync(listener)
                    llmInferenceSession!!.close()

                } catch (e: Exception) {
                    Log.e(TAG, "Error during session creation or generation", e)
                    close(e)
                } finally {
                    try {
                        llmInferenceSession!!.close()
                        llmInferenceSession = null
                    } catch (closeError: Exception) {
                        Log.w(TAG, "Error while closing session", closeError)
                    }
                }
            }

            awaitClose {
                job.cancel()
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
