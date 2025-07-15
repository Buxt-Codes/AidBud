package com.aidbud.data.viewmodel.repo

import com.aidbud.data.conversation.Conversation
import com.aidbud.data.conversation.ConversationDao
import com.aidbud.data.message.Message
import com.aidbud.data.message.MessageDao
import com.aidbud.data.pcard.PCard
import com.aidbud.data.pcard.PCardDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing all data operations related to Conversations, Messages, and PCards.
 * It abstracts the data sources (DAOs) from the ViewModel.
 */
@Singleton
class AidBudRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val pCardDao: PCardDao
) {
    // --- Conversation Operations ---
    fun getAllConversations(): Flow<List<Conversation>> = conversationDao.getAllConversations()
    fun getConversationById(conversationId: Long): Flow<Conversation?> = conversationDao.getConversationById(conversationId)
    suspend fun insertConversation(conversation: Conversation): Long = conversationDao.insertConversation(conversation)
    suspend fun updateConversation(conversation: Conversation) = conversationDao.updateConversation(conversation)
    suspend fun deleteConversation(conversation: Conversation) = conversationDao.deleteConversation(conversation)

    // --- Message Operations ---
    fun getMessagesForConversation(conversationId: Long): Flow<List<Message>> = messageDao.getMessagesForConversation(conversationId)
    fun getMessageById(messageId: Long): Flow<Message?> = messageDao.getMessageById(messageId)
    suspend fun insertMessage(message: Message): Long = messageDao.insertMessage(message)
    suspend fun updateMessage(message: Message) = messageDao.updateMessage(message)
    suspend fun deleteMessage(message: Message) = messageDao.deleteMessage(message)
    suspend fun deleteMessagesForConversation(conversationId: Long) = messageDao.deleteMessagesForConversation(conversationId)

    // --- PCard Operations ---
    fun getPCardsForConversation(conversationId: Long): Flow<List<PCard>> = pCardDao.getPCardsForConversation(conversationId)
    fun getPCardById(pCardId: Long): Flow<PCard?> = pCardDao.getPCardById(pCardId)
    suspend fun insertPCard(pCard: PCard): Long = pCardDao.insertPCard(pCard)
    suspend fun updatePCard(pCard: PCard) = pCardDao.updatePCard(pCard)
    suspend fun deletePCard(pCard: PCard) = pCardDao.deletePCard(pCard)
    suspend fun deletePCardsForConversation(conversationId: Long) = pCardDao.deletePCardsForConversation(conversationId)
}