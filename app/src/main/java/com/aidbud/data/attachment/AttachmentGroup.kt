package com.aidbud.data.attachment

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import com.aidbud.data.conversation.Conversation

@Entity(
    tableName = "attachment_groups",
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["conversation_id"], // Column in the Conversation table
            childColumns = ["conversation_id"],   // Column in the Message table
            onDelete = ForeignKey.CASCADE // If a Conversation is deleted, delete its messages
        )
    ]
)
data class AttachmentGroup(
    @PrimaryKey(autoGenerate = true) // Conversation ID, primary key, auto-generated
    @ColumnInfo(name = "attachment_group_id")
    val attachmentGroupId: Long = 0, // Default value for auto-generation

    @ColumnInfo(name = "conversation_id", index = true) // Foreign key, index for faster lookups
    val conversationId: Long,

    @ColumnInfo(name = "attachments")
    val attachments: List<Uri>,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "transcription")
    val transcription: String?,

    // lastUpdated will be set programmatically before insertion/update
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long // Timestamp (e.g., System.currentTimeMillis())
)