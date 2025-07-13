package com.aidbud.data.pcard

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.aidbud.data.conversation.Conversation // Import the Conversation entity

@Entity(
    tableName = "pcards",
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["conversation_id"], // Column in the Conversation table
            childColumns = ["conversation_id"],   // Column in the Message table
            onDelete = ForeignKey.CASCADE // If a Conversation is deleted, delete its messages
        )
    ]
)
data class PCard(
    @PrimaryKey(autoGenerate = true) // Message ID, primary key, auto-generated
    @ColumnInfo(name = "pcard_id")
    val pCardId: Long = 0,

    @ColumnInfo(name = "conversation_id", index = true) // Foreign key, index for faster lookups
    val conversationId: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    // Store images/videos as a JSON string of paths/URIs
    @ColumnInfo(name = "triage_level")
    val triageLevel: String?, // Optional

    @ColumnInfo(name = "injury_identification")
    val injuryIdentification: String?, // Optional

    // "Input changes" and "Output changes" can be stored as JSON strings if they are structured data
    @ColumnInfo(name = "identified_injury_description")
    val identifiedInjuryDescription: String?, // Optional

    @ColumnInfo(name = "patient_injury_description")
    val patientInjuryDescription: String?, // Optional

    @ColumnInfo(name = "intervention_plan")
    val interventionPlan: String? // Optional
)