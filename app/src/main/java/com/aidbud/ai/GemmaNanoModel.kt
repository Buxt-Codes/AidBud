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
import com.google.ai.edge.localagents.rag.chains.ChainConfig
import com.google.ai.edge.localagents.rag.chains.RetrievalAndInferenceChain
import com.google.ai.edge.localagents.rag.memory.DefaultSemanticTextMemory
import com.google.ai.edge.localagents.rag.memory.SqliteVectorStore
import com.google.ai.edge.localagents.rag.models.AsyncProgressListener
import com.google.ai.edge.localagents.rag.models.Embedder
import com.google.ai.edge.localagents.rag.models.GeckoEmbeddingModel
import com.google.ai.edge.localagents.rag.models.LanguageModelResponse
import com.google.ai.edge.localagents.rag.models.MediaPipeLlmBackend
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.ai.edge.localagents.rag.prompt.PromptBuilder
import com.google.ai.edge.localagents.rag.retrieval.RetrievalConfig
import com.google.ai.edge.localagents.rag.retrieval.RetrievalConfig.TaskType
import com.google.ai.edge.localagents.rag.retrieval.RetrievalRequest
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import kotlinx.coroutines.coroutineScope
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import java.util.concurrent.ConcurrentHashMap
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.common.util.concurrent.FutureCallback
import android.content.ContentValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.contains
import kotlin.math.max
import org.json.JSONObject
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.guava.await

import com.aidbud.ai.ModelResponse
import com.aidbud.ai.ModelResponseParser
import com.aidbud.data.viewmodel.repo.AidBudRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

val MAX_IMAGES = 64
var MAX_TOKENS = 2048

var DECODE_TOKEN_OFFSET = 256
var MODEL_PATH = ""
var FRAMES_PER_SECOND = 1
var GECKO_MODEL_PATH = ""
var TOKENIZER_MODEL_PATH = ""


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
class GemmaNanoModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AidBudRepository
) {

    private var llmInference: LlmInference? = null
    private var llmInferenceSession: LlmInferenceSession? = null
    private var inferenceOptions = LlmInference.LlmInferenceOptions.builder()
        .setModelPath(MODEL_PATH)
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

    private val embedder: Embedder<String> = GeckoEmbeddingModel(
            GECKO_MODEL_PATH,
            Optional.of(TOKENIZER_MODEL_PATH),
            false,
        )

    private val managerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentChatOperationJob: Job? = null

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

    fun initialiseRAG(
        conversationId: Long
    ): DefaultSemanticTextMemory {
        return DefaultSemanticTextMemory(SqliteVectorStore(768, "rag_$conversationId.db"), embedder)
    }

    suspend fun runRAG(conversationId: Long, query: String, timeoutMillis: Long = 5000L): List<String>? {
        return withContext(Dispatchers.IO) {
            val config = RetrievalConfig.create(3, 0.5f, TaskType.RETRIEVAL_QUERY)
            val request = RetrievalRequest.create(query, config)
            val conversation = repository.getConversationById(conversationId).first()
                ?: throw IllegalArgumentException("Conversation not found for id $conversationId")

            val semanticMemory = conversation.semanticMemory
                ?: throw IllegalStateException("SemanticMemory is null for conversation $conversationId")

            val future = semanticMemory.retrieveResults(request)
            val response = withTimeoutOrNull(timeoutMillis) { future.await() }

            response?.entities?.map { it.data }
        }
    }

    suspend fun insertRAG(conversationId: Long, data: String, timeoutMillis: Long = 5000L): Boolean? {
        return withContext(Dispatchers.IO) {
            val conversation = repository.getConversationById(conversationId).first()
                ?: throw IllegalArgumentException("Conversation not found for id $conversationId")

            val semanticMemory = conversation.semanticMemory
                ?: throw IllegalStateException("SemanticMemory is null for conversation $conversationId")
            val future = semanticMemory.recordMemoryItem(data)

            val result = withTimeoutOrNull(timeoutMillis) {
                future.await()
            }
            result
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
