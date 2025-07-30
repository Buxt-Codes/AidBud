package com.aidbud.data.message

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.aidbud.data.conversation.Conversation // Import the Conversation entity

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["conversation_id"], // Column in the Conversation table
            childColumns = ["conversation_id"],   // Column in the Message table
            onDelete = ForeignKey.CASCADE // If a Conversation is deleted, delete its messages
        )
    ]
)
data class Message(
    @PrimaryKey(autoGenerate = true) // Message ID, primary key, auto-generated
    @ColumnInfo(name = "message_id")
    val messageId: Long = 0,

    @ColumnInfo(name = "conversation_id", index = true) // Foreign key, index for faster lookups
    val conversationId: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name= "role")
    val role: String,

    // Store images/videos as a JSON string of paths/URIs
    @ColumnInfo(name = "attachments")
    val attachments: List<Uri>?, // Optional

    @ColumnInfo(name = "text")
    val text: String?, // Optional

    // "Input changes" and "Output changes" can be stored as JSON strings if they are structured data
    @ColumnInfo(name = "pcard_changes")
    val pCardChanges: String?, // Optional
)