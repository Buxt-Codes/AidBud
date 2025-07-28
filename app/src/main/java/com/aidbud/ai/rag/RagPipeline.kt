package com.aidbud.ai.rag

import android.content.Context
import com.aidbud.data.viewmodel.repo.AidBudRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedderResult

val BACKEND = Delegate.CPU
const val MODEL_PATH = ""

class RagPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AidBudRepository
) {
    private var textEmbedder: TextEmbedder? = null

    companion object {
        private const val TAG = "RagPipeline"
    }

    init {
        initialise()
    }

    fun initialise() {
        try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setDelegate(BACKEND)
                .setModelAssetPath(MODEL_PATH)
            val baseOptions = baseOptionsBuilder.build()
            val options = TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .build()
            val textEmbedder = TextEmbedder.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e(TAG, "Load model error: ${e.message}", e)
        }
    }

    suspend fun retrieveTextData(
        query: String,
        conversation_id: Long,
        topK: Int = 3,
        threshold: Float = 0.5f,
        timeoutMillis: Long = 5000L
    ): List<String> {

    }

    suspend fun retrieveAttachmentData(
        query: String,
        conversation_id: Long,
        topK: Int = 3,
        threshold: Float = 0.5f,
        timeoutMillis: Long = 5000L
    ): List<String> {

    }

    suspend fun retrieveData(
        query: String,
        conversation_id: Long,
        topK: Int = 3,
        threshold: Float = 0.5f,
        timeoutMillis: Long = 5000L
    ): List<String> {

    }

    suspend fun insertData(data: Map<String, Any>): Long {

    }

    suspend fun deleteData(id: Long): Boolean {

    }
}