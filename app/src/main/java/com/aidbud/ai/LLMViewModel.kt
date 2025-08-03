package com.aidbud.ai

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.aidbud.ai.llm.GemmaNanoModel
import com.aidbud.ai.llm.ModelResponse
import com.aidbud.ai.prompt.PromptBuilder
import com.aidbud.ai.rag.RagPipeline
import com.aidbud.ai.vosk.VideoTranscriber
import com.aidbud.data.message.Message
import com.aidbud.data.ragdata.RagData
import com.aidbud.data.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.aidbud.data.pcard.PCard

import com.aidbud.data.viewmodel.repo.AidBudRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class LLMViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AidBudRepository,
    private val settings: SettingsViewModel
) {
    private val llm = GemmaNanoModel(context)
    private val rag = RagPipeline(context, repository)
    private val promptBuilder = PromptBuilder(context, repository, settings)
    private val transcriber = VideoTranscriber(context, "vosk/vosk-model-small-en-us-0.15")
    private val TAG = "LLMViewModel"
    private val _currentState = MutableStateFlow(ModelResponse())
    val currentState: StateFlow<ModelResponse> = _currentState.asStateFlow()
    private var currentJob: Job? = null
    private var responseJob: Job? = null
    private var conversationId: Long? = null
    private var query: String? = null

    suspend fun transcribe(
        attachments: List<Uri>
    ): String? = coroutineScope {
            if (attachments.isEmpty()) {
                Log.d("VideoTranscriber", "No attachments to transcribe.")
                return@coroutineScope null
            }

            val transcriptionJobs = attachments.map { uri ->
                async(Dispatchers.IO) {
                    try {
                        val transcription = transcriber.transcribeVideo(uri)
                        transcription ?: "Transcription for ${uri.lastPathSegment}: No speech detected."

                    } catch (e: IllegalStateException) {
                        Log.e("VideoTranscriber", "Vosk model not initialized for $uri: ${e.message}", e)
                        "Error transcribing ${uri.lastPathSegment}: Vosk model failed to initialize."
                    } catch (e: IOException) {
                        Log.e("VideoTranscriber", "I/O error for $uri: ${e.message}", e)
                        "Error transcribing ${uri.lastPathSegment}: Failed to extract audio."
                    } catch (e: Exception) {
                        Log.e("VideoTranscriber", "Unexpected error for $uri: ${e.message}", e)
                        "Error transcribing ${uri.lastPathSegment}: ${e.message}"
                    }
                }
            }

            transcriptionJobs.awaitAll().joinToString("\n")
        }

    suspend fun attachmentProcessing(
        conversationId: Long,
        query: String,
        attachments: List<Uri> = emptyList(),
    ): Map<String, String?> {
        if (attachments.isEmpty()) {
            Log.d("AttachmentProcessor", "No attachments provided.")
            return mapOf("error" to "No attachment was processed as the list was empty.")
        }

        val videoAttachments = attachments.filter { uri ->
            val mimeType = context.contentResolver.getType(uri) ?: ""
            mimeType.startsWith("video/") && hasAudioTrack(context, uri)
        }
        var transcription: String? = null
        if (!videoAttachments.isEmpty()) {
            transcription = transcribe(videoAttachments)
        }

        val prompt = promptBuilder.buildAttachmentPrompt(query, transcription)
        val response = llm.generateResponse(prompt, attachments).first()
        val json = extractJSON(response)

        val description = json?.let { JSONObject(it).getString("description") }

        if (description == null && transcription == null) {
            return mapOf("error" to "Error in generating both attachment descriptions and transcriptions.")
        }

        val data: MutableMap<String, String> = mutableMapOf()
        if (description != null) {
            data["description"] = description
        }
        if (transcription != null) {
            data["transcription"] = transcription
        }

        rag.insertData(
            data = data,
            attachments = attachments,
            conversationId = conversationId
        )

        return data
    }

    private fun hasAudioTrack(context: Context, uri: Uri): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            hasAudio == "yes"
        } catch (e: Exception) {
            Log.e("AudioTrackChecker", "Failed to retrieve metadata for $uri", e)
            false
        } finally {
            retriever.release()
        }
    }

    private fun extractJSON(input: String): String? {
        val regex = Regex("""(?s)\{[^{}]*}""")
        val matchResult = regex.find(input)

        if (matchResult != null) {
            val potentialJson = matchResult.value
            try {
                JSONObject(potentialJson)
                return potentialJson
            } catch (e: JSONException) {
                Log.e("JsonParser", "Found a string that looks like JSON, but it's invalid: $potentialJson", e)
            }
        }
        return null
    }

    fun run(
        query: String,
        attachments: List<Uri> = emptyList(),
        conversationId: Long
    ) {
        cancel()
        this.query = query
        this.conversationId = conversationId
        _currentState.update { it.copy(isLoading = true, errorMessage = null, isCompleted = false) }
        currentJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                if (attachments.isEmpty()) {
                    generateFunctionQueryResponse(query, conversationId)
                } else {
                    val ragText = rag.retrieveTextData(query, conversationId)
                    val ragAttachment = rag.retrieveAttachmentData(query, conversationId)
                    generateResponse(
                        query,
                        attachments,
                        conversationId,
                        ragText,
                        ragAttachment
                    )
                }



            } catch (e: Exception) {
                cancel()
                Log.e(TAG, "Error: $e", e)
                _currentState.update {
                    it.copy(
                        isLoading = false,
                        isCompleted = true,
                        errorMessage = "There was an error in generating a response"
                    )
                }
            }

            if (_currentState.value.generatedText != "") {
                repository.insertMessage(
                    Message(
                        conversationId = conversationId,
                        timestamp = System.currentTimeMillis(),
                        role = "LLM",
                        text = _currentState.value.generatedText,
                        attachments = null,
                        pCardChanges = _currentState.value.pCardData
                    )
                )

                val currentConversation = repository.getConversationById(conversationId).first()
                if (currentConversation != null) {
                    repository.updateConversation(currentConversation.copy(lastUpdated = System.currentTimeMillis()))
                }

                rag.insertData(
                    data = mapOf(
                        "query" to query,
                        "response" to _currentState.value.generatedText,
                        "pCardChanges" to (_currentState.value.pCardData?.toString() ?: "")
                    ),
                    attachments = emptyList(),
                    conversationId = conversationId
                )
            }

            _currentState.update { it.copy(
                generatedText = "",
                pCardData = null,
                isPCardActive = false,
                functionData = null,
                isFunctionCall = false,
                isLoading = true,
                errorMessage = null,
                isCompleted = false)
            }

        }
    }

    private suspend fun generateFunctionQueryResponse(
        query: String,
        conversationId: Long,
    ) {
        val ragText = rag.retrieveTextData(query, conversationId)
        val ragAttachment = rag.retrieveAttachmentData(query, conversationId)
        val prompt = promptBuilder.buildQueryWithFunctionPrompt(
            query,
            conversationId,
            ragText,
            ragAttachment
        )

        responseJob = CoroutineScope(Dispatchers.Main).launch {
            llm.generateResponsePCard(prompt).collect { modelResponse ->
                if (modelResponse.isFunctionCall && modelResponse.functionData != null) {
                    Log.d(
                        TAG,
                        "Function call detected. Cancelling current flow and generating function response."
                    )
                    responseJob?.cancel()
                    generateFunctionResponse(
                        query,
                        conversationId,
                        modelResponse.functionData,
                        ragText,
                        ragAttachment
                    )
                } else {
                    _currentState.update {
                        it.copy(
                            isLoading = true,
                            generatedText = modelResponse.generatedText,
                            pCardData = modelResponse.pCardData,
                            isPCardActive = modelResponse.isPCardActive,
                            isFunctionCall = modelResponse.isFunctionCall
                        )
                    }
                }
            }
        }

        _currentState.update {
            it.copy(
                isLoading = false,
                isCompleted = true,
                errorMessage = null
            )
        }
    }

    private suspend fun generateFunctionResponse(
        query: String,
        conversationId: Long,
        functionData: JSONObject,
        ragText: List<RagData>,
        ragAttachment: List<RagData>
    ) {
        val attachmentRagId = functionData.optLong("attachment", -1L)
        if (attachmentRagId == -1L) {
            val attachmentRagData = repository.getRagDataById(attachmentRagId).firstOrNull()
            if (attachmentRagData == null) {
                generateResponse(
                    query,
                    conversationId = conversationId,
                    ragText = ragText,
                    ragAttachment = ragAttachment
                )
            } else {
                val attachmentDescription = attachmentRagData.data["description"] as String
                val attachmentTranscription = attachmentRagData.data["transcription"] as String

                val prompt = promptBuilder.buildFunctionPrompt(
                    query,
                    attachmentRagId,
                    attachmentDescription,
                    attachmentTranscription,
                    functionData.optString("remarks", null),
                    conversationId,
                    ragText,
                    ragAttachment
                )

                var pcardupdate = false
                responseJob = CoroutineScope(Dispatchers.Main).launch {
                    llm.generateResponsePCard(prompt).collect { modelResponse ->
                        if (modelResponse.pCardData != null && !pcardupdate) {
                            pcardupdate = true

                            val triageLevel = modelResponse.pCardData.optString("triage_level", null)
                            val injuryIdentification = modelResponse.pCardData.optString("injury_identification", null)
                            val identifiedInjuryDescription = modelResponse.pCardData.optString("identified_injury_description", null)
                            val patientInjuryDescription = modelResponse.pCardData.optString("patient_injury_description", null)
                            val interventionPlan = modelResponse.pCardData.optString("intervention_plan", null)
                            val currentPCard = repository.getPCardsForConversation(conversationId).firstOrNull()?.firstOrNull()
                            if (currentPCard != null) {
                                repository.updatePCard(
                                    currentPCard.copy(
                                        triageLevel = triageLevel ?: currentPCard.triageLevel,
                                        injuryIdentification = injuryIdentification ?: currentPCard.injuryIdentification,
                                        identifiedInjuryDescription = identifiedInjuryDescription ?: currentPCard.identifiedInjuryDescription,
                                        patientInjuryDescription = patientInjuryDescription ?: currentPCard.patientInjuryDescription,
                                        interventionPlan = interventionPlan ?: currentPCard.interventionPlan
                                    )
                                )
                            } else {
                                repository.insertPCard(
                                    PCard(
                                        conversationId = conversationId,
                                        timestamp = System.currentTimeMillis(),
                                        triageLevel = triageLevel,
                                        injuryIdentification = injuryIdentification,
                                        identifiedInjuryDescription = identifiedInjuryDescription,
                                        patientInjuryDescription = patientInjuryDescription,
                                        interventionPlan = interventionPlan
                                    )
                                )
                            }

                            val attachmentNewDescription = functionData.optString("new_attachment_description", null)
                            if (attachmentNewDescription != null) {
                                val updatedData = attachmentRagData.data.toMutableMap()
                                updatedData["description"] = attachmentNewDescription
                                rag.updateData(
                                    attachmentRagId,
                                    updatedData
                                )
                            }

                        }
                        _currentState.update {
                            it.copy(
                                isLoading = true,
                                generatedText = modelResponse.generatedText,
                                pCardData = modelResponse.pCardData,
                                isPCardActive = modelResponse.isPCardActive,
                                isFunctionCall = false
                            )
                        }
                    }
                }

                _currentState.update {
                    it.copy(
                        isLoading = false,
                        isCompleted = true,
                        errorMessage = null
                    )
                }
            }
        } else {
            generateResponse(
                query,
                conversationId = conversationId,
                ragText = ragText,
                ragAttachment = ragAttachment
            )
        }
    }

    private suspend fun generateResponse(
        query: String,
        attachments: List<Uri> = emptyList(),
        conversationId: Long,
        ragText: List<RagData>,
        ragAttachment: List<RagData>
    ) {
        var attachmentDescription: String? = null
        var attachmentTranscription: String? = null
        var attachmentError: String? = null
        if (!attachments.isEmpty()) {
            val attachmentData = attachmentProcessing(conversationId, query, attachments)
            attachmentDescription = attachmentData["description"]
            attachmentTranscription = attachmentData["transcription"]
            attachmentError = attachmentData["error"]
        }

        if (attachmentError != null) {

            val prompt = promptBuilder.buildQueryPrompt(
                query,
                attachmentDescription,
                attachmentTranscription,
                conversationId,
                ragText,
                ragAttachment
            )

            responseJob = CoroutineScope(Dispatchers.Main).launch {
                llm.generateResponsePCard(prompt).collect { modelResponse ->
                    _currentState.update {
                        it.copy(
                            isLoading = true,
                            generatedText = modelResponse.generatedText,
                            pCardData = modelResponse.pCardData,
                            isPCardActive = modelResponse.isPCardActive
                        )
                    }
                }
            }

            _currentState.update {
                it.copy(
                    isLoading = false,
                    isCompleted = true,
                    errorMessage = null
                )
            }
        } else {
            _currentState.update {
                it.copy(
                    isLoading = false,
                    isCompleted = true,
                    errorMessage = attachmentError
                )
            }
        }
    }

    fun cancel() {
        if (responseJob?.isActive == true) {
            responseJob?.cancel()
        }
        if (currentJob?.isActive == true) {
            currentJob?.cancel()
            _currentState.update {
                it.copy(
                    isLoading = false,
                    isCompleted = false,
                    errorMessage = "Generation cancelled by user."
                )
            }
        }

        if (_currentState.value.generatedText != "" && conversationId != null && query != null) {
            CoroutineScope(Dispatchers.IO).launch {
                repository.insertMessage(
                    Message(
                        conversationId = conversationId!!,
                        timestamp = System.currentTimeMillis(),
                        role = "LLM",
                        text = _currentState.value.generatedText,
                        attachments = null,
                        pCardChanges = _currentState.value.pCardData
                    )
                )

                val currentConversation = repository.getConversationById(conversationId!!).first()
                if (currentConversation != null) {
                    repository.updateConversation(currentConversation.copy(lastUpdated = System.currentTimeMillis()))
                }

                rag.insertData(
                    data = mapOf(
                        "query" to query!!,
                        "response" to _currentState.value.generatedText,
                        "pCardChanges" to (_currentState.value.pCardData?.toString() ?: "")
                    ),
                    attachments = emptyList(),
                    conversationId = conversationId!!
                )
            }

            conversationId = null
            query = null
        }

        _currentState.update { it.copy(
            generatedText = "",
            pCardData = null,
            isPCardActive = false,
            functionData = null,
            isFunctionCall = false,
            isLoading = true,
            errorMessage = null,
            isCompleted = false)
        }
    }
}