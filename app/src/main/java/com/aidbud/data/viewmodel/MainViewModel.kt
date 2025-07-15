package com.aidbud.data.viewmodel


import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidbud.data.conversation.Conversation
import com.aidbud.data.message.Message
import com.aidbud.data.pcard.PCard
import com.aidbud.data.viewmodel.repo.AidBudRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * AidBudViewModel serves as the central ViewModel for the application, managing all data
 * interactions through the AidBudRepository and handling LLM inference streaming.
 *
 * It exposes data as StateFlows for Compose UI to observe reactively.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AidBudRepository
) : ViewModel() {

    // --- LLM Streaming State ---
    // MutableSharedFlow to emit real-time updates from the LLM inference.
    // This is a "hot" flow, suitable for events and streaming data.
    private val _llmResponseStream = MutableSharedFlow<LLMResponseState>()
    val llmResponseStream: SharedFlow<LLMResponseState> = _llmResponseStream

    // --- Conversation Operations ---

    /**
     * Exposes a StateFlow of all conversations, ordered by last_updated.
     * `stateIn` converts the cold Flow from the repository into a hot StateFlow,
     * which is efficient for UI observation.
     */
    val allConversations: StateFlow<List<Conversation>> =
        repository.getAllConversations()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Keep active for 5s after last collector
                initialValue = emptyList()
            )

    /**
     * Retrieves a single conversation by its ID. Returns a Flow that emits the Conversation
     * or null if not found.
     */
    fun getConversationById(conversationId: Long): Flow<Conversation?> {
        return repository.getConversationById(conversationId)
    }

    /**
     * Inserts a new conversation into the database.
     * @param title The title of the new conversation.
     * @return The ID of the newly inserted conversation.
     */
    suspend fun insertConversation(title: String): Long {
        val conversation = Conversation(title = title, lastUpdated = System.currentTimeMillis())
        return repository.insertConversation(conversation)
    }

    /**
     * Updates an existing conversation in the database. The last_updated timestamp is
     * automatically updated to the current time.
     * @param conversation The Conversation object to update.
     */
    fun updateConversation(conversation: Conversation) {
        viewModelScope.launch {
            repository.updateConversation(conversation.copy(lastUpdated = System.currentTimeMillis()))
        }
    }

    /**
     * Deletes a conversation from the database.
     * @param conversation The Conversation object to delete.
     */
    fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            repository.deleteConversation(conversation)
        }
    }

    // --- Message Operations ---

    /**
     * Retrieves all messages for a specific conversation ID.
     * Returns a Flow that updates whenever messages for this conversation change.
     * @param conversationId The ID of the conversation to retrieve messages for.
     */
    fun getMessagesForConversation(conversationId: Long): Flow<List<Message>> {
        return repository.getMessagesForConversation(conversationId)
    }

    /**
     * Retrieves a single message by its ID. Returns a Flow that emits the Message or null.
     * @param messageId The ID of the message to retrieve.
     */
    fun getMessageById(messageId: Long): Flow<Message?> {
        return repository.getMessageById(messageId)
    }

    /**
     * Inserts a new message into the database.
     * Optionally updates the parent conversation's last_updated timestamp.
     * @param conversationId The ID of the conversation this message belongs to.
     * @param role The role of the message (e.g., "user", "ai", "system").
     * @param text The text content of the message (optional).
     * @param photosAndVideos A list of URIs for associated media (optional).
     * @param pCardChanges A JSON string representing pCard changes (optional).
     */
    fun insertMessage(
        conversationId: Long,
        role: String,
        text: String?,
        photosAndVideos: List<Uri>? = null,
        pCardChanges: String? = null
    ) {
        viewModelScope.launch {
            val message = Message(
                conversationId = conversationId,
                timestamp = System.currentTimeMillis(),
                role = role,
                text = text,
                photosAndVideos = photosAndVideos,
                pCardChanges = pCardChanges
            )
            repository.insertMessage(message)
            // Update parent conversation's timestamp to bring it to top of list
            repository.getConversationById(conversationId).collect { conversation ->
                conversation?.let {
                    repository.updateConversation(it.copy(lastUpdated = System.currentTimeMillis()))
                }
            }
        }
    }

    /**
     * Updates an existing message in the database.
     * @param message The Message object to update.
     */
    fun updateMessage(message: Message) {
        viewModelScope.launch {
            repository.updateMessage(message)
        }
    }

    /**
     * Deletes a message from the database.
     * @param message The Message object to delete.
     */
    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            repository.deleteMessage(message)
        }
    }

    /**
     * Deletes all messages associated with a specific conversation.
     * @param conversationId The ID of the conversation whose messages should be deleted.
     */
    fun deleteMessagesForConversation(conversationId: Long) {
        viewModelScope.launch {
            repository.deleteMessagesForConversation(conversationId)
        }
    }


    // --- PCard Operations ---

    /**
     * Retrieves all PCards for a specific conversation ID.
     * Returns a Flow that updates whenever PCards for this conversation change.
     * @param conversationId The ID of the conversation to retrieve PCards for.
     */
    fun getPCardsForConversation(conversationId: Long): Flow<List<PCard>> {
        return repository.getPCardsForConversation(conversationId)
    }

    /**
     * Retrieves a single PCard by its ID. Returns a Flow that emits the PCard or null.
     * @param pCardId The ID of the PCard to retrieve.
     */
    fun getPCardById(pCardId: Long): Flow<PCard?> {
        return repository.getPCardById(pCardId)
    }

    /**
     * Inserts a new PCard into the database.
     * Optionally updates the parent conversation's last_updated timestamp.
     * @param conversationId The ID of the conversation this PCard belongs to.
     * @param triageLevel The triage level (optional).
     * @param injuryIdentification The identified injury (optional).
     * @param identifiedInjuryDescription Description of the identified injury (optional).
     * @param patientInjuryDescription Description of the patient's injury (optional).
     * @param interventionPlan The intervention plan (optional).
     */
    fun insertPCard(
        conversationId: Long,
        triageLevel: String? = null,
        injuryIdentification: String? = null,
        identifiedInjuryDescription: String? = null,
        patientInjuryDescription: String? = null,
        interventionPlan: String? = null
    ) {
        viewModelScope.launch {
            val pCard = PCard(
                conversationId = conversationId,
                timestamp = System.currentTimeMillis(),
                triageLevel = triageLevel,
                injuryIdentification = injuryIdentification,
                identifiedInjuryDescription = identifiedInjuryDescription,
                patientInjuryDescription = patientInjuryDescription,
                interventionPlan = interventionPlan
            )
            repository.insertPCard(pCard)
            // Update parent conversation's timestamp to bring it to top of list
            repository.getConversationById(conversationId).collect { conversation ->
                conversation?.let {
                    repository.updateConversation(it.copy(lastUpdated = System.currentTimeMillis()))
                }
            }
        }
    }

    /**
     * Updates an existing PCard in the database.
     * @param pCard The PCard object to update.
     */
    fun updatePCard(pCard: PCard) {
        viewModelScope.launch {
            repository.updatePCard(pCard)
        }
    }

    /**
     * Deletes a PCard from the database.
     * @param pCard The PCard object to delete.
     */
    fun deletePCard(pCard: PCard) {
        viewModelScope.launch {
            repository.deletePCard(pCard)
        }
    }

    /**
     * Deletes all PCards associated with a specific conversation.
     * @param conversationId The ID of the conversation whose PCards should be deleted.
     */
    fun deletePCardsForConversation(conversationId: Long) {
        viewModelScope.launch {
            repository.deletePCardsForConversation(conversationId)
        }
    }

    /**
     * Initiates an LLM inference request and streams the response.
     * The chunks are emitted via `llmResponseStream`.
     * Once complete, the full response is saved as a new message.
     *
     * @param prompt The user's input prompt for the LLM.
     * @param conversationId The ID of the conversation to associate this interaction with.
     */
    fun generateLlmResponseStream(prompt: String, conversationId: Long) {
        viewModelScope.launch {
            _llmResponseStream.emit(LLMResponseState.Loading)
            var fullResponse = StringBuilder()

            // First, save the user's message to the database
            insertMessage(conversationId, "user", prompt)

            try {
                // Fetch chat history for context (if needed for LLM)
                val chatHistory = repository.getMessagesForConversation(conversationId).stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily, // Only collect when needed
                    initialValue = emptyList()
                ).value // Get current messages
                    .map { message ->
                        JSONObject().apply {
                            put("role", message.role)
                            val partsArray = JSONArray()
                            message.text?.let { partsArray.put(JSONObject().put("text", it)) }
                            // Add other parts like inlineData for images if needed
                            put("parts", partsArray)
                        }
                    }
                // Add the current user prompt to history for the LLM call
                val userPromptPart = JSONObject().apply {
                    put("role", "user")
                    val partsArray = JSONArray().put(JSONObject().put("text", prompt))
                    put("parts", partsArray)
                }
                val payloadContents = JSONArray(chatHistory).put(userPromptPart)


                val payload = JSONObject().apply {
                    put("contents", payloadContents)
                }

                // API Key is left empty as Canvas will provide it at runtime
                val apiKey = ""
                val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

                val response = fetch(apiUrl, payload.toString())

                if (response.isSuccessful) {
                    val resultJson = JSONObject(response.body)
                    val candidates = resultJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val textPart = parts.optJSONObject(0)?.optString("text")
                            if (textPart != null) {
                                // Simulate streaming by breaking the full response into chunks
                                val chunkSize = 10 // Emit 10 characters at a time
                                for (i in textPart.indices step chunkSize) {
                                    val chunk = textPart.substring(i, (i + chunkSize).coerceAtMost(textPart.length))
                                    fullResponse.append(chunk)
                                    _llmResponseStream.emit(LLMResponseState.Chunk(chunk))
                                    delay(50) // Simulate network delay for streaming effect
                                }
                                _llmResponseStream.emit(LLMResponseState.Complete)

                                // Save the full LLM response as a new message in the database
                                insertMessage(conversationId, "model", fullResponse.toString())
                            } else {
                                _llmResponseStream.emit(LLMResponseState.Error("LLM response text part not found."))
                            }
                        } else {
                            _llmResponseStream.emit(LLMResponseState.Error("LLM response parts not found."))
                        }
                    } else {
                        _llmResponseStream.emit(LLMResponseState.Error("LLM candidates not found."))
                    }
                } else {
                    _llmResponseStream.emit(LLMResponseState.Error("LLM API error: ${response.statusCode} - ${response.body}"))
                }
            } catch (e: Exception) {
                _llmResponseStream.emit(LLMResponseState.Error("Failed to generate LLM response: ${e.message}"))
            }
        }
    }

    // Helper function to simulate fetch API call (replace with actual fetch in browser environment)
    private suspend fun fetch(url: String, body: String): FetchResponse {
        // In a real Android app, you'd use a library like OkHttp or Ktor for network requests.
        // For this environment, we'll simulate a network call.
        // This is a placeholder for the actual fetch call in the Canvas environment.
        // The actual fetch call will be done by the Canvas runtime.
        // We'll mimic the structure of the fetch response.
        return try {
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // This is where the actual network call would happen in a real app.
                // For the purpose of this example, we'll return a dummy response.
                // In a Canvas environment, you'd directly use the provided fetch API.
                // Example of a dummy response:
                val dummyResponseText = "Hello! I am Gemini. How can I assist you today? I can help with information, creative writing, and much more."
                FetchResponse(true, 200, """{"candidates":[{"content":{"parts":[{"text":"$dummyResponseText"}]}}]}""")
            }
            response
        } catch (e: Exception) {
            FetchResponse(false, -1, "Network error: ${e.message}")
        }
    }

    // Data class to represent a simplified fetch response for this example
    private data class FetchResponse(val isSuccessful: Boolean, val statusCode: Int, val body: String)
}