package com.aidbud.di

import android.content.Context
import androidx.room.Room
import com.aidbud.data.AppDatabase
import com.aidbud.data.conversation.ConversationDao
import com.aidbud.data.message.MessageDao
import com.aidbud.data.pcard.PCardDao
import com.aidbud.data.ragdata.RagDataDao
import com.aidbud.data.viewmodel.repo.AidBudRepository
import com.aidbud.data.settings.SettingsDataStore // Import the new SettingsDataStore class
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAidBudDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "aidbud_database"
        ).build()
    }

    @Provides
    fun provideConversationDao(database: AppDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    fun providePCardDao(database: AppDatabase): PCardDao {
        return database.pCardDao()
    }

    @Provides
    fun provideRagDataDao(database: AppDatabase): RagDataDao {
        return database.ragDataDao()
    }

    @Provides
    @Singleton
    fun provideAidBudRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        pCardDao: PCardDao,
        ragDataDao: RagDataDao
    ): AidBudRepository {
        return AidBudRepository(conversationDao, messageDao, pCardDao, ragDataDao)
    }

    // Provide SettingsDataStore as a singleton
    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
}