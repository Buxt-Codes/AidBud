package com.aidbud.data.pcard

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface PCardDao {
    @Query("SELECT * FROM pcards WHERE conversation_id = :conversationId ORDER BY pcard_id DESC")
    fun getPCardsForConversation(conversationId: Long): Flow<List<PCard>>

    @Query("SELECT * FROM pcards WHERE pcard_id = :pCardId")
    fun getPCardById(pCardId: Long): Flow<PCard?>

    @Insert
    suspend fun insertPCard(pCard: PCard): Long

    @Update
    suspend fun updatePCard(pCard: PCard)

    @Delete
    suspend fun deletePCard(pCard: PCard)

    @Query("DELETE FROM pcards WHERE conversation_id = :conversationId")
    suspend fun deletePCardsForConversation(conversationId: Long)
}