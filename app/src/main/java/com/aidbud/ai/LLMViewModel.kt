package com.aidbud.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.aidbud.ai.llm.GemmaNanoModel
import com.aidbud.ai.prompt.PromptBuilder
import com.aidbud.ai.rag.RagPipeline
import com.aidbud.ai.vosk.VideoTranscriber
import com.aidbud.data.ragdata.RagData
import com.aidbud.data.settings.SettingsViewModel
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

import com.aidbud.data.viewmodel.repo.AidBudRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class LLMViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AidBudRepository,
    private val settings: SettingsViewModel
) {
    private val llm = GemmaNanoModel(context)
    private val rag = RagPipeline(context, repository)
    private val promptBuilder = PromptBuilder(context, repository, settings)
    private val transcriber = VideoTranscriber(context, "vosk/vosk-model-small-en-us-0.15")

    suspend fun attachmentProcessing(
        conversationId: Long,
        query: String,
        additionalComment: String? = null,
        attachments: List<Uri> = emptyList(),
        ragDataId: Long? = null
    ): Long {

        val prompt = promptBuilder.buildAttachmentPrompt(query, additionalComment, null, ragDataId)
        val response = llm.generateResponse(prompt, attachments)

        if (ragDataId != null) {
            val ragData = repository.getRagDataById(ragDataId).firstOrNull()
            if (ragData != null) {
                val updatedData = ragData.data.toMutableMap()
                updatedData["description"] = response
                rag.updateData(
                    id = ragDataId,
                    data = updatedData
                )
                return ragDataId
            } else {
                Log.e("attachmentProcessing", "RAG data not found for ID: $ragDataId")
                return -1
            }
        } else {
            return rag.insertData(
                data = mapOf("description" to response),
                attachments = attachments,
                conversationId = conversationId
            )
        }
    }

    suspend fun generateResponse(
        query: String,
        conversationId: Long
    ) {

    }
}