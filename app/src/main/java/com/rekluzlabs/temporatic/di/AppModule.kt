package com.rekluzlabs.temporatic.di

import android.content.Context
import com.rekluzlabs.temporatic.data.ScreenshotDatabase
import com.rekluzlabs.temporatic.data.ScreenshotRepository
import com.rekluzlabs.temporatic.capture.ScreenCaptureEngine
import com.rekluzlabs.temporatic.manager.*
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
    fun provideScreenshotDatabase(
        @ApplicationContext context: Context
    ): ScreenshotDatabase = ScreenshotDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideScreenshotRepository(
        @ApplicationContext context: Context,
        storageManager: StorageManager
    ): ScreenshotRepository = ScreenshotRepository(context, storageManager)

    @Provides
    @Singleton
    fun provideStorageManager(
        @ApplicationContext context: Context
    ): StorageManager = StorageManager(context)

    @Provides
    @Singleton
    fun provideScreenshotManager(
        @ApplicationContext context: Context
    ): ScreenshotManager = ScreenshotManager(context)

    @Provides
    @Singleton
    fun provideFlashManager(
        @ApplicationContext context: Context
    ): FlashManager = FlashManager(context)

    @Provides
    @Singleton
    fun provideScreenCaptureEngine(
        @ApplicationContext context: Context
    ): ScreenCaptureEngine = ScreenCaptureEngine(context)
}
