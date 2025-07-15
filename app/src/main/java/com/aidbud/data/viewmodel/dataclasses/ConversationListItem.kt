package com.aidbud.data.viewmodel.dataclasses

/**
 * Represents a simplified view of a conversation for a list display.
 */
data class ConversationListItem(
    val conversationId: Long,
    val title: String,
    val lastUpdated: Long
)