package com.aidbud.data.conversation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY last_updated DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE conversation_id = :conversationId")
    fun getConversationById(conversationId: Long): Flow<Conversation?>

    @Insert
    suspend fun insertConversation(conversation: Conversation): Long

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Delete
    suspend fun deleteConversation(conversation: Conversation)
}