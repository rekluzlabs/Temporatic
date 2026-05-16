package com.rekluzlabs.temporatic.utils

import android.content.Context
import android.graphics.Bitmap
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
}
