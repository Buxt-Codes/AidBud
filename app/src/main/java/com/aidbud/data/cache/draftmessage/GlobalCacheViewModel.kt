package com.aidbud.data.cache.draftmessage

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel

class GlobalCacheViewModel : ViewModel() {
    private val MAX_VIDEO_DURATION_MILLIS = 30_000L

    private var _currentConversationId: Int? = null
    private val _drafts = mutableStateMapOf<Int, DraftMessage>()
    val drafts: SnapshotStateMap<Int, DraftMessage> = _drafts

    fun updateText(conversationId: Int, text: String) {
        val current = _drafts[conversationId] ?: DraftMessage()
        _drafts[conversationId] = current.copy(text = text)
    }

    fun addAttachment(context: Context, conversationId: Int, uri: Uri) {
        val current = _drafts[conversationId] ?: DraftMessage()
        val durationMillis = getVideoDurationMillis(context, uri)

        val newAttachments = current.attachments + uri
        val newDuration = current.totalVideoDurationMillis + durationMillis

        _drafts[conversationId] = current.copy(
            attachments = newAttachments,
            totalVideoDurationMillis = newDuration
        )
    }

    fun removeAttachment(context: Context, conversationId: Int, uri: Uri) {
        val current = _drafts[conversationId] ?: DraftMessage()
        val durationMillis = getVideoDurationMillis(context, uri)

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

    fun getCurrentConversationId(): Int? = _currentConversationId

    fun setCurrentConversationId(conversationId: Int) {
        _currentConversationId = conversationId
    }

    private fun getVideoDurationMillis(context: Context, uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}