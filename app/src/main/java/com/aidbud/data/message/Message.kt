package com.aidbud.data.message

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

    // Store images/videos as a JSON string of paths/URIs
    @ColumnInfo(name = "input_photos_videos")
    val inputPhotosAndVideos: String?, // Optional

    @ColumnInfo(name = "input_text")
    val inputText: String?, // Optional

    // "Input changes" and "Output changes" can be stored as JSON strings if they are structured data
    @ColumnInfo(name = "input_changes")
    val inputChanges: String?, // Optional

    @ColumnInfo(name = "output_text")
    val outputText: String?, // Optional

    @ColumnInfo(name = "output_changes")
    val outputChanges: String? // Optional
)