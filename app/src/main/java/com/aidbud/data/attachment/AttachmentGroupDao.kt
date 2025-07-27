package com.aidbud.data.attachment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow
import android.net.Uri

@Dao
interface AttachmentGroupDao {
    @Query("SELECT * FROM attachment_groups WHERE conversation_id = :conversationId ORDER BY attachment_group_id DESC")
    fun getAttachmentGroupsForConversation(conversationId: Long): Flow<List<AttachmentGroup>>

    @Query("SELECT * FROM attachment_groups WHERE attachment_group_id = :attachmentGroupId")
    fun getAttachmentGroupsById(attachmentGroupId: Long): Flow<AttachmentGroup?>

    @Query("SELECT * FROM attachment_groups WHERE attachments = :attachments AND conversation_id = :conversationId ORDER BY attachment_group_id DESC LIMIT 1")
    fun getAttachmentGroupsByAttachments(attachments: List<Uri>, conversationId: Long): Flow<AttachmentGroup>

    @Insert
    suspend fun insertAttachmentGroup(attachmentGroup: AttachmentGroup): Long

    @Update
    suspend fun updateAttachmentGroup(attachmentGroup: AttachmentGroup)

    @Delete
    suspend fun deleteAttachmentGroup(attachmentGroup: AttachmentGroup)

    @Query("DELETE FROM attachment_groups WHERE conversation_id = :conversationId")
    suspend fun deleteAttachmentGroupsForConversation(conversationId: Long)
}