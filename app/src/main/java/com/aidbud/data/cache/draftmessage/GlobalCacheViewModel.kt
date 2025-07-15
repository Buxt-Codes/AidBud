package com.aidbud.data.cache.draftmessage

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * ViewModel for managing global cache, specifically draft messages with attachments.
 * Context is injected via Hilt for media operations.
 */
@HiltViewModel
class GlobalCacheViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context // Inject Context via Hilt
) : ViewModel() {

    private val MAX_VIDEO_DURATION_MILLIS = 30_000L

    // Using mutableStateMapOf directly for Compose observation
    private val _drafts = mutableStateMapOf<Int, DraftMessage>()
    val drafts: SnapshotStateMap<Int, DraftMessage> = _drafts

    // Using a simple mutableStateOf for current conversation ID, as it's not persisted
    private var _currentConversationId: Int? by mutableStateOf(null)
    val currentConversationId: Int?
        get() = _currentConversationId

    fun updateText(conversationId: Int, text: String) {
        val current = _drafts[conversationId] ?: DraftMessage()
        _drafts[conversationId] = current.copy(text = text)
    }

    fun addAttachment(conversationId: Int, uri: Uri) {
        val current = _drafts[conversationId] ?: DraftMessage()
        val durationMillis = getVideoDurationMillis(uri) // Use injected context

        val newAttachments = current.attachments + uri
        val newDuration = current.totalVideoDurationMillis + durationMillis

        _drafts[conversationId] = current.copy(
            attachments = newAttachments,
            totalVideoDurationMillis = newDuration
        )
    }

    fun removeAttachment(conversationId: Int, uri: Uri) {
        val current = _drafts[conversationId] ?: DraftMessage()
        val durationMillis = getVideoDurationMillis(uri) // Use injected context

        val newAttachments = current.attachments - uri
        val newDuration = (current.totalVideoDurationMillis - durationMillis).coerceAtLeast(0L)

        _drafts[conversationId] = current.copy(
            attachments = newAttachments,
            totalVideoDurationMillis = newDuration
        )
    }

    fun getDraft(conversationId: Int): DraftMessage {
        return _drafts[conversationId] ?: DraftMessage()
    }

    fun clearDraft(conversationId: Int) {
        _drafts.remove(conversationId)
    }

    fun getAttachmentCount(conversationId: Int): Int {
        val draft = _drafts[conversationId]
        return draft?.attachments?.size ?: 0
    }

    fun getDurationLeft(conversationId: Int): Long {
        val used = _drafts[conversationId]?.totalVideoDurationMillis ?: 0L
        return (MAX_VIDEO_DURATION_MILLIS - used).coerceAtLeast(0L)
    }

    fun setCurrentConversationId(conversationId: Int) {
        _currentConversationId = conversationId
    }

    private fun getVideoDurationMillis(uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            // Use the injected applicationContext here
            retriever.setDataSource(applicationContext, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}