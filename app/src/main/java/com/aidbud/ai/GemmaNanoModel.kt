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
import kotlin.math.max

val MAX_IMAGES = 64
var MAX_TOKENS = 2048

var DECODE_TOKEN_OFFSET = 256
var MODEL_PATH = ""

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

    private lateinit var llmInference: LlmInference
    private lateinit var llmInferenceSession: LlmInferenceSession

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

    fun generateResponseAsync(prompt: String, progressListener: ProgressListener<String>) : ListenableFuture<String> {
        llmInferenceSession.addQueryChunk(prompt)
        return llmInferenceSession.generateResponseAsync(progressListener)
    }

    fun generateResponseAsyncAttachment(prompt: String, attachments: List<Uri>, progressListener: ProgressListener<String>) : ListenableFuture<String> {
        llmInferenceSession.addQueryChunk(prompt)
        attachments.forEach({
        }

                llmInferenceSession.addImage(BitmapImageBuilder(it).build())
    })

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
//    fun generateResponse(
//        prompt: String,
//        attachments: List<Uri> = emptyList()
//    ): Flow<String> {
//        val inferenceInstance = llmInference
//            ?: throw IllegalStateException("LlmInferenceService is not initialized. Call initialize() first.")
//
//        return callbackFlow {
//            val job = launch(Dispatchers.IO) {
//                // Define session options, enabling the vision modality.
//                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
//                    .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
//                    .setTemperature(0.7f)
//                    .setTopK(40)
//                    .build()
//
//                // Use a try-with-resources block to ensure the session is closed automatically.
//                try {
//                    inferenceInstance.createSession(sessionOptions).use { session ->
//                        // 1. Process all URIs to get a flat list of bitmaps.
//                        val bitmaps = processUris(attachments)
//
//                        // 2. Add the text prompt first, as recommended for better performance.
//                        session.addQueryChunk(prompt)
//
//                        // 3. Convert each bitmap to MPImage and add it to the session.
//                        bitmaps.forEach { bitmap ->
//                            val mpImage = BitmapImageBuilder(bitmap).build()
//                            session.addImage(mpImage)
//                        }
//
//                        // 4. Define the result listener for streaming output.
//                        val resultListener = object : LlmInference.LlmInferenceResultListener {
//                            override fun onResult(result: LlmInference.PartialResult) {
//                                trySend(result.partialResult)
//                            }
//
//                            override fun onError(error: RuntimeException, isFinished: Boolean) {
//                                Log.e(TAG, "Inference error", error)
//                                close(error)
//                            }
//
//                            override fun onCompletion() {
//                                close()
//                            }
//                        }
//
//                        // 5. Start the asynchronous generation process on the session.
//                        session.generateResponseAsync(resultListener)
//                    }
//                } catch (e: Exception) {
//                    Log.e(TAG, "Error during session creation or generation", e)
//                    close(e) // Close the flow with an exception if processing fails.
//                }
//            }
//
//            awaitClose {
//                job.cancel()
//            }
//        }
//    }

    /**
     * Processes a list of URIs, converting images and video frames into Bitmaps.
     */
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

    /**
     * Extracts a fixed number of frames from a video URI using MediaMetadataRetriever.
     */
    private fun extractFramesFromVideo(uri: Uri): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0

            if (durationMs > 0) {
                val intervalUs = (durationMs * 1000) / FRAMES_PER_SECOND
                for (i in 0 until (FRAMES_PER_SECOND * (durationMs * 1000))) {
                    val timeUs = i * intervalUs
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
        return frames
    }

    /**
     * Releases resources used by the LlmInference engine.
     */
    fun close() {
        llmInference?.close()
        llmInference = null
    }
}

/**
 * A helper object to contain common MIME types for easy checking.
 */
object MimeType {
    val IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp")
    val VIDEO_TYPES = setOf("video/mp4", "video/3gpp", "video/webm", "video/avi")
}
