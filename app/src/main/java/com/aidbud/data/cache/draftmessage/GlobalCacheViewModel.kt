package com.aidbud.data.cache.draftmessage

import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel

class GlobalCacheViewModel : ViewModel() {
    // Key = conversationId
    private val _drafts = mutableStateMapOf<String, DraftMessage>()
    val drafts: SnapshotStateMap<String, DraftMessage> = _drafts

    fun updateText(conversationId: String, text: String) {
        val current = _drafts[conversationId] ?: DraftMessage()
        _drafts[conversationId] = current.copy(text = text)
    }

    fun addAttachment(conversationId: String, uri: Uri) {
        val current = _drafts[conversationId] ?: DraftMessage()
        _drafts[conversationId] = current.copy(attachments = current.attachments + uri)
    }

    fun removeAttachment(conversationId: String, uri: Uri) {
        val current = _drafts[conversationId] ?: DraftMessage()
        _drafts[conversationId] = current.copy(attachments = current.attachments - uri)
    }

    fun getDraft(conversationId: String): DraftMessage {
        return _drafts[conversationId] ?: DraftMessage()
    }

    fun clearDraft(conversationId: String) {
        _drafts.remove(conversationId)
    }
}