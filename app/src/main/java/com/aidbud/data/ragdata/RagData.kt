package com.aidbud.data.ragdata

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import com.aidbud.data.conversation.Conversation

@Entity(
    tableName = "rag_data",
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["conversation_id"], // Column in the Conversation table
            childColumns = ["conversation_id"],   // Column in the Message table
            onDelete = ForeignKey.CASCADE // If a Conversation is deleted, delete its messages
        )
    ]
)
data class RagData(
    @PrimaryKey(autoGenerate = true) // Conversation ID, primary key, auto-generated
    @ColumnInfo(name = "rag_data_id")
    val ragDataId: Long = 0, // Default value for auto-generation

    @ColumnInfo(name = "conversation_id", index = true) // Foreign key, index for faster lookups
    val conversationId: Long,

    @ColumnInfo(name = "data")
    val data: Map<String, Any>,

    @ColumnInfo(name = "attachments")
    val attachments: List<Uri>?,

    @ColumnInfo(name = "embedding")
    val embedding: List<Float>,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long
)