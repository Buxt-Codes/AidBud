package com.aidbud.ai.rag

import com.google.ai.edge.localagents.rag.models.EmbedData
import com.google.ai.edge.localagents.rag.models.GeckoEmbeddingModel
import com.google.ai.edge.localagents.rag.models.Embedder
import com.google.ai.edge.localagents.rag.models.EmbeddingRequest
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import java.util.Optional
import java.util.concurrent.Executor // Or use Android's ContextCompat.getMainExecutor(context)
import kotlin.collections.toFloatArray
import kotlin.math.sqrt

/**
 * A wrapper for the GeckoEmbeddingModel that provides embeddings as FloatArray.
 * This simplifies working with the embedding results in Kotlin.
 *
 * @param embeddingModelPath The file path to the Gecko TFLite model (.tflite).
 * @param sentencePieceModelPath The optional file path to the SentencePiece tokenizer model (.model).
 * If not provided, the model is assumed to contain its own tokenizer.
 * @param useGpu A boolean indicating whether to use the GPU for inference (true) or CPU (false).
 */
class GeckoEmbedder(
    private val embeddingModelPath: String,
    private val sentencePieceModelPath: Optional<String>,
    private val useGpu: Boolean
) {

    private lateinit var geckoEmbedder: Embedder<String>

    init {
        initialise()
    }

    fun initialise() {
        geckoEmbedder = GeckoEmbeddingModel(
            embeddingModelPath,
            sentencePieceModelPath,
            useGpu
        )
    }

    suspend fun embed(text: String): FloatArray {
        val embedData: EmbedData<String> = EmbedData.create(
            text,
            EmbedData.TaskType.SEMANTIC_SIMILARITY
        )
        val request: EmbeddingRequest<String> = EmbeddingRequest.create(
            ImmutableList.of(embedData)
        )

        val immutableList: ImmutableList<Float> = geckoEmbedder.getEmbeddings(request).await()

        return withContext(Dispatchers.Default) {
            immutableList.let {
                val floatArray = it.toFloatArray()
                val sumOfSquares = floatArray.sumOf { x -> x.toDouble() * x.toDouble() }
                val l2Norm = sqrt(sumOfSquares).toFloat()
                if (l2Norm == 0f) {
                    FloatArray(floatArray.size) { 0f }
                } else {
                    floatArray.map { value -> value / l2Norm }.toFloatArray()
                }
            }
        } ?: FloatArray(0)
    }

    fun cosineSimilarity(embed1: FloatArray, embed2: FloatArray): Float {
        if (embed1.size != embed2.size) {
            throw IllegalArgumentException("Embeddings must have the same dimension to compute cosine similarity.")
        }
        if (embed1.isEmpty()) {
            return 0f
        }

        var dotProduct = 0.0f
        for (i in embed1.indices) {
            dotProduct += embed1[i] * embed2[i]
        }
        return dotProduct
    }
}