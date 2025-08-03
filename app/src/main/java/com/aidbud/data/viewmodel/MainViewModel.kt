package com.aidbud.data.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidbud.ai.LLMViewModel
import com.aidbud.ai.llm.ModelResponse
import com.aidbud.data.conversation.Conversation
import com.aidbud.data.downloader.DownloadState
import com.aidbud.data.downloader.ModelDownloader
import com.aidbud.data.message.Message
import com.aidbud.data.pcard.PCard
import com.aidbud.data.settings.SettingsDataStore
import com.aidbud.data.viewmodel.repo.AidBudRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

/**
 * AidBudViewModel serves as the central ViewModel for the application, managing all data
 * interactions through the AidBudRepository and handling LLM inference streaming.
 *
 * It exposes data as StateFlows for Compose UI to observe reactively.
 */
private const val HUGGING_FACE_MODEL_URL = "https://huggingface.co/buxtcodes/gemma3n4b-aidbud/blob/main/gemma3n4b-aidbud.task"

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val repository: AidBudRepository,
    private val settingsDataStore: SettingsDataStore,
    private val llm: LLMViewModel
) : ViewModel() {

    val llmState: StateFlow<ModelResponse> = llm.currentState

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    val conversationLimit: StateFlow<Int> = settingsDataStore.conversationLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 100)

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
        return repository.insertConversation(Conversation(title = title,  lastUpdated = System.currentTimeMillis()))
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
        attachments: List<Uri>? = null,
        pCardChanges: JSONObject? = null
    ) {
        viewModelScope.launch {
            val message = Message(
                conversationId = conversationId,
                timestamp = System.currentTimeMillis(),
                role = role,
                text = text,
                attachments = attachments,
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

    fun runLLM(
        query: String,
        attachments: List<Uri> = emptyList(),
        conversationId: Long
    ) {
        llm.run(query, attachments, conversationId)
    }

    fun cancelLLM() {
        llm.cancel()
    }

    fun getModelFile(): File {
        return File(applicationContext.filesDir, "gemma3n4b-aidbud.task")
    }

    fun isModelDownloaded(): Boolean {
        return getModelFile().exists() && getModelFile().length() > 0
    }

    // New function to expose the download flow
    suspend fun startDownload() {
        // Prevent starting a new download if one is already in progress
        if (_downloadState.value is DownloadState.Loading) return

        // Set the state to Idle to trigger a UI update and new download flow
        _downloadState.value = DownloadState.Idle

        // Use a coroutine scope to launch the download in a background thread
        // In a real ViewModel, this would be `viewModelScope.launch(Dispatchers.IO)`
        viewModelScope.launch(Dispatchers.IO) {
            ModelDownloader.downloadFile(HUGGING_FACE_MODEL_URL, getModelFile())
                .collect { state ->
                    _downloadState.value = state
                }
        }
    }
}