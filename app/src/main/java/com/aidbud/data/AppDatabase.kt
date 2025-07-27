package com.aidbud.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // Import TypeConverters
import com.aidbud.data.conversation.Conversation
import com.aidbud.data.conversation.ConversationDao
import com.aidbud.data.message.Message // Import the new Message entity
import com.aidbud.data.message.MessageDao // Import the new MessageDao
import com.aidbud.data.pcard.PCard // Import the new PCard entity
import com.aidbud.data.pcard.PCardDao // Import the new PCardDao
import com.aidbud.data.attachment.AttachmentGroup // Import the new AttachmentGroup entity
import com.aidbud.data.attachment.AttachmentGroupDao // Import the new AttachmentGroupDao
import com.aidbud.data.converters.AppTypeConverters // Import your Type Converters object

@Database(
    entities = [
        Conversation::class,
        Message::class,
        PCard::class,
        AttachmentGroup::class
   ], // <--- ADD Message::class here
    version = 1, // <--- IMPORTANT: Increment database version
    exportSchema = true
)
@TypeConverters(AppTypeConverters::class) // <--- ADD THIS to register your converters
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao // <--- ADD THIS for your MessageDao
    abstract fun pCardDao(): PCardDao // <--- ADD THIS for your PCardDao
    abstract fun attachmentGroupDao(): AttachmentGroupDao // <--- ADD THIS for your PCardDao
}