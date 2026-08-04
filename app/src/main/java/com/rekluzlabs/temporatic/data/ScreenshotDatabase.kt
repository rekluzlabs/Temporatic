package com.rekluzlabs.temporatic.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScreenshotRecord::class], version = 4, exportSchema = false)
abstract class ScreenshotDatabase : RoomDatabase() {
    abstract fun screenshotDao(): ScreenshotDao

    companion object {
        @Volatile
        private var instance: ScreenshotDatabase? = null

        fun getInstance(context: Context): ScreenshotDatabase {
            val existing = instance
            if (existing != null) return existing
            return synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScreenshotDatabase::class.java,
                    "temporatic_screenshots.db"
                ).fallbackToDestructiveMigration(false).build().also { instance = it }
            }
        }
    }
}
