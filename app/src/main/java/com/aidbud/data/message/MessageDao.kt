package com.aidbud.data.message

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY message_id DESC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE message_id = :messageId")
    fun getMessageById(messageId: Long): Flow<Message?>

    @Insert
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("DELETE FROM messages WHERE conversation_id = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)
}