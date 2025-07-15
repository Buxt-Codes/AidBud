package com.aidbud.data.viewmodel.dataclasses

import com.aidbud.data.conversation.Conversation
import com.aidbud.data.message.Message
import com.aidbud.data.pcard.PCard

data class ConversationDetail(
    val conversation: Conversation,
    val messages: List<Message>,
    val pcards: List<PCard>
)
