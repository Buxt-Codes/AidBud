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

    suspend fun transcribe(
        attachments: List<Uri>
    ) {

    }

    suspend fun attachmentProcessing(
        conversationId: Long,
        query: String,
        additionalComment: String? = null,
        attachments: List<Uri> = emptyList(),
        ragDataId: Long? = null
    ): Map<String, String> {
        if (attachments) {
            // empty attachment list processing return error saying that no attachment was processed
        }

        // check if any videos are present
        // transcribing the attachments and compile into a string

        val prompt = promptBuilder.buildAttachmentPrompt(query, additionalComment, null, ragDataId)
        val response = llm.generateResponse(prompt, attachments)

        // parse the response to extract just the descriptions

        if (ragDataId != null) {
            val ragData = repository.getRagDataById(ragDataId).firstOrNull()
            if (ragData != null) {
                val updatedData = ragData.data.toMutableMap()
                updatedData["description"] = response
                rag.updateData(
                    id = ragDataId,
                    data = updatedData
                )
            } else {
                Log.e("attachmentProcessing", "RAG data not found for ID: $ragDataId")
            }
        } else {
            rag.insertData(
                data = mapOf("description" to response),
                attachments = attachments,
                conversationId = conversationId
            )
        }
        // return the description & transcription in either a map or a pair
    }

    suspend fun generateResponse(
        query: String,
        attachments: List<Uri> = emptyList(),
        conversationId: Long
    ) {
        // start all this in a couroutine that is cancellable by a seperate function

        // if there are attachments, process the group of attachments
        // get the description & transcription pair here

        // conduct rag here for the prompt building // add a new function in the prompt building
        // build the prompt here

        // start generating the prompt here in a flow that updates a data object
        // make it such that as we return the flow, we also
    }
}