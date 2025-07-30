package com.aidbud.ai.rag

import android.content.Context
import android.net.Uri
import com.aidbud.data.viewmodel.repo.AidBudRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.util.Log

import com.aidbud.data.ragdata.RagData
import com.google.mediapipe.tasks.components.containers.Embedding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Optional

val BACKEND_GPU = false
const val EMBEDDER_MODEL_PATH = "embedder/Gecko_1024_quant.tflite"
const val TOKENISER_MODEL_PATH = "tokeniser/sentencepiece.model"

class RagPipeline(
    private val context: Context,
    private val repository: AidBudRepository
) {
    private var textEmbedder: GeckoEmbedder? = null

    companion object {
        private const val TAG = "RagPipeline"
    }

    init {
        initialise()
    }

    fun initialise() {
        try {
            textEmbedder = GeckoEmbedder(
                EMBEDDER_MODEL_PATH,
                Optional.of(TOKENISER_MODEL_PATH),
                BACKEND_GPU
            )
        } catch (e: Exception) {
            Log.e(TAG, "Load model error: ${e.message}", e)
        }
    }

    suspend fun retrieveTextData(
        query: String,
        conversationId: Long,
        topK: Int = 3,
        threshold: Float = 0.5f,
        timeoutMillis: Long = 5000L
    ): List<RagData> {
        val result = withTimeoutOrNull(timeoutMillis) {
            try {
                if (textEmbedder == null) {
                    initialise()
                }

                val result = textEmbedder!!.embed(query)
                val entries = repository.getRagText(conversationId).first()

                if (entries.isEmpty()) {
                    return@withTimeoutOrNull emptyList()
                }

                return@withTimeoutOrNull entries
                    .mapNotNull { rag ->
                        try {
                            val similarity = textEmbedder!!.cosineSimilarity(
                                result,
                                rag.embedding
                            )
                            if (similarity >= threshold) rag to similarity else null
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error calculating similarity for RAG entry data. Error: ${e.message}",
                                e
                            )
                            null
                        }
                    }
                    .sortedByDescending { it.second }
                    .take(topK)
                    .map { it.first }

            } catch (e: Exception) {
                Log.e("retrieveTextData", "An error occurred during text data retrieval for query '$query': ${e.message}", e)
                emptyList() // Return empty list on any caught error
            }
        }

        return result ?: emptyList()
    }

    suspend fun retrieveAttachmentData(
        query: String,
        conversationId: Long,
        topK: Int = 3,
        threshold: Float = 0.5f,
        timeoutMillis: Long = 5000L
    ): List<RagData> {
        val result = withTimeoutOrNull(timeoutMillis) {
            try {
                if (textEmbedder == null) {
                    initialise()
                }

                val result = textEmbedder!!.embed(query)
                val entries = repository.getRagAttachment(conversationId).first()

                if (entries.isEmpty()) {
                    return@withTimeoutOrNull emptyList()
                }

                return@withTimeoutOrNull entries
                    .mapNotNull { rag ->
                        try {
                            val similarity = textEmbedder!!.cosineSimilarity(
                                result,
                                rag.embedding
                            )
                            if (similarity >= threshold) rag to similarity else null
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error calculating similarity for RAG entry data. Error: ${e.message}",
                                e
                            )
                            null
                        }
                    }
                    .sortedByDescending { it.second }
                    .take(topK)
                    .map { it.first }

            } catch (e: Exception) {
                // Catch any exception that occurs within the entire suspend block (except individual mapNotNull errors)
                Log.e("retrieveTextData", "An error occurred during text data retrieval for query '$query': ${e.message}", e)
                emptyList() // Return empty list on any caught error
            }
        }

        return result ?: emptyList()
    }

    suspend fun retrieveData(
        query: String,
        conversationId: Long,
        topK: Int = 3,
        threshold: Float = 0.5f,
        timeoutMillis: Long = 5000L
    ): List<RagData> {
        val result = withTimeoutOrNull(timeoutMillis) {
            try {
                if (textEmbedder == null) {
                    initialise()
                }

                val result = textEmbedder!!.embed(query)
                val entries = repository.getRagDataForConversation(conversationId).first()

                if (entries.isEmpty()) {
                    return@withTimeoutOrNull emptyList()
                }

                return@withTimeoutOrNull entries
                    .mapNotNull { rag ->
                        try {
                            val similarity = textEmbedder!!.cosineSimilarity(
                                result,
                                rag.embedding
                            )
                            if (similarity >= threshold) rag to similarity else null
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error calculating similarity for RAG entry data. Error: ${e.message}",
                                e
                            )
                            null
                        }
                    }
                    .sortedByDescending { it.second }
                    .take(topK)
                    .map { it.first }

            } catch (e: Exception) {
                Log.e("retrieveTextData", "An error occurred during text data retrieval for query '$query': ${e.message}", e)
                emptyList() // Return empty list on any caught error
            }
        }

        return result ?: emptyList()
    }

    suspend fun insertData(
        data: Map<String, Any>,
        attachments: List<Uri>?,
        conversationId: Long,
        timeoutMillis: Long = 5000L
    ): Long {
        val result = withTimeoutOrNull(timeoutMillis) {
            try {
                if (textEmbedder == null) {
                    initialise()
                }

                val inputText = data.entries.joinToString(" ") { "${it.key}: ${it.value}" }
                val embeddingResult = textEmbedder!!.embed(inputText)
                val embedding = embeddingResult

                val ragData = RagData(
                    data = data,
                    attachments = attachments,
                    conversationId = conversationId,
                    embedding = embedding,
                    lastUpdated = System.currentTimeMillis()
                )

                return@withTimeoutOrNull repository.insertRagData(ragData)
            } catch (e: Exception) {
                Log.e("insertData", "An error occurred during data insertion: ${e.message}", e)
                return@withTimeoutOrNull -1
            }
        }

        return result ?: -1
    }

    suspend fun deleteData(
        id: Long,
        timeoutMillis: Long = 5000L
    ): Boolean {
        val result = withTimeoutOrNull(timeoutMillis) {
            try {
                repository.deleteRagData(
                    repository.getRagDataById(id).firstOrNull() ?: return@withTimeoutOrNull false
                )
                return@withTimeoutOrNull true
            } catch (e: Exception) {
                Log.e("deleteData", "An error occurred during data deletion: ${e.message}", e)
                return@withTimeoutOrNull false
            }
        }

        return result ?: false
    }

    suspend fun updateData(
        id: Long,
        data: Map<String, Any>? = null,
        attachments: List<Uri>? = null,
        conversationId: Long? = null,
        timeoutMillis: Long = 5000L
    ): Boolean {
        val result = withTimeoutOrNull(timeoutMillis) {
            try {
                val ragData = repository.getRagDataById(id).firstOrNull() ?: return@withTimeoutOrNull false
                repository.updateRagData(
                    ragData.copy(
                        data = data ?: ragData.data,
                        attachments = attachments ?: ragData.attachments,
                        conversationId = conversationId ?: ragData.conversationId,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
                return@withTimeoutOrNull true
            } catch (e: Exception) {
                Log.e("deleteData", "An error occurred during data deletion: ${e.message}", e)
                return@withTimeoutOrNull false
            }
        }

        return result ?: false
    }
}