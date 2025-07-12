package com.aidbud.data.conversation

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey(autoGenerate = true) // Conversation ID, primary key, auto-generated
    @ColumnInfo(name = "conversation_id")
    val conversationId: Long = 0, // Default value for auto-generation

    @ColumnInfo(name = "title")
    val title: String,

    // Store images/videos as a JSON string of paths/URIs
    @ColumnInfo(name = "current_images_videos")
    val currentImagesAndVideos: String?, // Nullable if optional

    @ColumnInfo(name = "current_text_input")
    val currentTextInput: String?, // Nullable if optional

    // lastUpdated will be set programmatically before insertion/update
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long // Timestamp (e.g., System.currentTimeMillis())
)