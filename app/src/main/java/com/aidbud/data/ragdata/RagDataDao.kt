package com.aidbud.data.ragdata

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow
import android.net.Uri

@Dao
interface RagDataDao {
    @Query("SELECT * FROM rag_data WHERE conversation_id = :conversationId ORDER BY last_updated DESC")
    fun getRagDataForConversation(conversationId: Long): Flow<List<RagData?>>

    @Query("SELECT * FROM rag_data WHERE rag_data_id = :ragDataId")
    fun getRagDataById(ragDataId: Long): Flow<RagData?>

    @Query("SELECT * FROM rag_data WHERE attachments IS NULL AND conversation_id = :conversationId ORDER BY last_updated DESC")
    fun getRagText(conversationId: Long): Flow<List<RagData?>>

    @Query("SELECT * FROM rag_data WHERE attachments IS NOT NULL AND conversation_id = :conversationId ORDER BY last_updated DESC")
    fun getRagAttachment(conversationId: Long): Flow<List<RagData?>>

    @Insert
    suspend fun insertRagData(ragData: RagData): Long

    @Update
    suspend fun updateRagData(ragData: RagData)

    @Delete
    suspend fun deleteRagData(ragData: RagData)

    @Query("DELETE FROM rag_data WHERE conversation_id = :conversationId")
    suspend fun deleteRagDataForConversation(conversationId: Long)
}