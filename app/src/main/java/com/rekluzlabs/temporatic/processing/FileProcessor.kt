package com.rekluzlabs.temporatic.processing

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.rekluzlabs.temporatic.data.ScreenshotRecord
import com.rekluzlabs.temporatic.data.ScreenshotRepository
import com.rekluzlabs.temporatic.domain.ScreenshotEvent
import com.rekluzlabs.temporatic.manager.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileProcessor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: ScreenshotRepository,
    private val storageManager: StorageManager,
) {

    suspend fun processScreenshot(event: ScreenshotEvent.SystemScreenshotDetected) = withContext(Dispatchers.IO) {
        try {
            Log.d("FileProcessor", "Processing detection for: ${event.filename}")

            // 1. Copy the file to the custom folder
            val newUri = storageManager.copyScreenshot(event.fileUri)

            if (newUri != null) {
                Log.d("FileProcessor", "Successfully COPIED to custom folder: $newUri")

                val savedFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, newUri)
                val finalFilename = savedFile?.name ?: event.filename

                // 2. Create record for database with the NEW path
                val record = ScreenshotRecord(
                    timestamp = event.timestamp,
                    filePath = newUri.toString(),
                    filename = finalFilename
                )

                // Insert into database
                repository.insert(record)
                Log.d("FileProcessor", "Database record created")

                // 3. Try to delete the original system screenshot
                try {
                    val deleted = context.contentResolver.delete(event.fileUri, null, null)
                    Log.d("FileProcessor", "Original system file deleted: $deleted")
                } catch (e: Exception) {
                    Log.w("FileProcessor", "Could not delete original system screenshot (this is expected on some Android versions): ${e.message}")
                }
            } else {
                Log.e("FileProcessor", "FAILED to copy screenshot. Check if save folder is still accessible.")
            }

        } catch (e: Exception) {
            Log.e("FileProcessor", "Critical error in processScreenshot", e)
        }
    }
}
