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
    private val _drafts = mutableStateMapOf<Long, DraftMessage>()
    val drafts: SnapshotStateMap<Long, DraftMessage> = _drafts

    // Using a simple mutableStateOf for current conversation ID, as it's not persisted
    private var _currentConversationId: Long? by mutableStateOf(null)
    val currentConversationId: Long?
        get() = _currentConversationId

    fun updateText(conversationId: Long, text: String) {
        val current = _drafts[conversationId] ?: DraftMessage().also {
            _drafts[conversationId] = it
        }
        current.text = text
    }

    fun addAttachment(conversationId: Long, uri: Uri) {
        val current = _drafts[conversationId] ?: DraftMessage().also {
            _drafts[conversationId] = it
        }

        val durationMillis = getVideoDurationMillis(uri)

        current.attachments.add(uri)
        current.totalVideoDurationMillis += durationMillis
    }

    fun removeAttachment(conversationId: Long, uri: Uri) {
        val current = _drafts[conversationId] ?: return
        val durationMillis = getVideoDurationMillis(uri)

        current.attachments.remove(uri)
        current.totalVideoDurationMillis =
            (current.totalVideoDurationMillis - durationMillis).coerceAtLeast(0L)
    }

    fun getDraft(conversationId: Long): DraftMessage {
        return _drafts[conversationId] ?: DraftMessage()
    }

    fun clearDraft(conversationId: Long) {
        _drafts.remove(conversationId)
    }

    fun getAttachmentCount(conversationId: Long): Int {
        val draft = _drafts[conversationId]
        return draft?.attachments?.size ?: 0
    }

    fun getDurationLeft(conversationId: Long): Long {
        val used = _drafts[conversationId]?.totalVideoDurationMillis ?: 0L
        return (MAX_VIDEO_DURATION_MILLIS - used).coerceAtLeast(0L)
    }

    fun setCurrentConversationId(conversationId: Long) {
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