package com.rekluzlabs.temporatic.data.service

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ScreenshotObserver(
    handler: Handler,
    private val context: Context
) : ContentObserver(handler) {
    private var lastDetectedScreenshot: String? = null

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        uri?.let {
            if (it.toString().contains("content://media/external/images/media")) {
                val cursor = context.contentResolver.query(
                    it,
                    arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                    null, null, null
                )
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val displayName = c.getString(
                            c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                        )
                        if (displayName.lowercase().contains("screenshot") &&
                            lastDetectedScreenshot != displayName) {
                            lastDetectedScreenshot = displayName
                            moveScreenshot(displayName)
                        }
                    }
                }
            }
        }
    }

    private fun moveScreenshot(filename: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Find original file in DCIM/Screenshots or Pictures/Screenshots
                val dcimScreenshots = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    "Screenshots/$filename"
                )
                val picturesScreenshots = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Screenshots/$filename"
                )
                
                val originalFile = when {
                    dcimScreenshots.exists() -> dcimScreenshots
                    picturesScreenshots.exists() -> picturesScreenshots
                    else -> null
                }
                
                if (originalFile != null && originalFile.exists()) {
                    // Create destination folder
                    val destFolder = File(
                        context.getExternalFilesDir(null),
                        "TemporaticScreenshots"
                    ).apply { mkdirs() }
                    
                    val destFile = File(destFolder, filename)
                    if (originalFile.renameTo(destFile)) {
                        Log.d("ScreenshotDetection", "Moved screenshot to: ${destFile.absolutePath}")
                    } else {
                        Log.e("ScreenshotDetection", "Failed to move screenshot")
                    }
                }
            } catch (e: Exception) {
                Log.e("ScreenshotDetection", "Error: ${e.message}")
            }
        }
    }
}
