package com.rekluzlabs.temporatic.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun saveBitmap(context: Context, bitmap: Bitmap): File? {
        val timestamp = DateTimeUtils.getFilenameTimestamp()
        val filename = "temporatic_$timestamp.png"
        
        val picturesDir = File(
            context.getExternalFilesDir(null), "TemporaticScreenshots"
        ).apply { mkdirs() }
        
        val file = File(picturesDir, filename)
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBitmapToCustomFolder(
        context: Context,
        bitmap: Bitmap,
        folderUri: Uri
    ): Boolean {
        return try {
            val parentFolder = DocumentFile.fromTreeUri(context, folderUri)
            if (parentFolder == null || !parentFolder.canWrite()) return false
            
            val timestamp = DateTimeUtils.getFilenameTimestamp()
            val filename = "temporatic_$timestamp.png"
            
            val file = parentFolder.createFile("image/png", filename)
            if (file != null) {
                context.contentResolver.openOutputStream(file.uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
