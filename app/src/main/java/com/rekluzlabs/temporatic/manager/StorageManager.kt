package com.rekluzlabs.temporatic.manager

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("temporatic", Context.MODE_PRIVATE)
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getSavedFolderUri(): Uri? {
        return prefs.getString("save_folder_uri", null)?.let { Uri.parse(it) }
    }

    fun saveFolderUri(uri: Uri) {
        prefs.edit().putString("save_folder_uri", uri.toString()).apply()
    }

    fun initializeDefaultDirectory() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val tempo = File(dcim, "Temporatic")
                if (!tempo.exists()) {
                    tempo.mkdirs()
                }
            } else {
                // On Android 10+, MediaStore will create it automatically on the first save.
                // We don't strictly need to do anything here, but we could insert and delete a placeholder 
                // if we wanted to force the folder to appear in file managers immediately.
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Failed to initialize default directory", e)
        }
    }

    fun saveScreenshot(bitmap: Bitmap, subfolder: String? = null): Uri? {
        val formatStr = prefs.getString("file_format", "PNG") ?: "PNG"
        val quality = prefs.getInt("image_quality", 100)
        val format = if (formatStr == "JPG") Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
        val mimeType = if (formatStr == "JPG") "image/jpeg" else "image/png"
        val extension = if (formatStr == "JPG") "jpg" else "png"
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.getDefault()).format(Date())
        val filename = "Tempo_$timestamp.$extension"

        // 1. Save a copy accessible via FileProvider for immediate share/crop
        var shareableUri: Uri? = null
        try {
            val internalDir = File(context.filesDir, "TemporaticScreenshots")
            internalDir.mkdirs()
            val internalFile = File(internalDir, filename)
            FileOutputStream(internalFile).use { bitmap.compress(format, quality, it) }
            shareableUri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", internalFile
            )
        } catch (e: Exception) {
            Log.e("StorageManager", "Failed to save internal shareable copy", e)
        }

        // 2. Save to external/public location (gallery) asynchronously
        ioScope.launch {
            try {
                val rootFolderUri = getSavedFolderUri()
                if (rootFolderUri == null) {
                    saveToMediaStore(bitmap, subfolder, filename, format, mimeType, quality)
                } else {
                    val rootDoc = DocumentFile.fromTreeUri(context, rootFolderUri)
                    if (rootDoc != null) {
                        val targetDoc = subfolder?.let { getOrCreateSubfolder(rootDoc, it) } ?: rootDoc
                        val file = targetDoc.createFile(mimeType, filename)
                        file?.uri?.let { uri ->
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                bitmap.compress(format, quality, outputStream)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StorageManager", "Failed to save external screenshot asynchronously", e)
            }
        }

        return shareableUri
    }

    private fun saveToMediaStore(bitmap: Bitmap, subfolder: String?, filename: String, format: Bitmap.CompressFormat, mimeType: String, quality: Int) {
        try {
            val relativePath = if (subfolder != null) {
                "${Environment.DIRECTORY_DCIM}/Temporatic/$subfolder"
            } else {
                "${Environment.DIRECTORY_DCIM}/Temporatic"
            }

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = context.contentResolver.insert(collection, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(format, quality, outputStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Failed to save screenshot via MediaStore", e)
        }
    }

    fun copyScreenshot(sourceUri: Uri, subfolder: String? = null): Uri? {
        val rootFolderUri = getSavedFolderUri()
        if (rootFolderUri == null) {
            return copyToMediaStore(sourceUri, subfolder)
        }

        try {
            val rootDoc = DocumentFile.fromTreeUri(context, rootFolderUri) ?: return null
            val targetDoc = subfolder?.let { getOrCreateSubfolder(rootDoc, it) } ?: rootDoc

            val sourceFile = DocumentFile.fromSingleUri(context, sourceUri)
            var filename = sourceFile?.name ?: "Screenshot_${System.currentTimeMillis()}.png"
            
            // Prefix with Tempo_ to avoid detection loop if not already present
            if (!filename.startsWith("Tempo_")) {
                filename = "Tempo_$filename"
            }

            val targetFile = targetDoc.createFile("image/png", filename) ?: return null

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                    input.copyTo(output)
                }
            }

            return targetFile.uri
        } catch (e: Exception) {
            Log.e("StorageManager", "Failed to copy screenshot", e)
        }
        return null
    }

    private fun copyToMediaStore(sourceUri: Uri, subfolder: String?): Uri? {
        try {
            val sourceFile = DocumentFile.fromSingleUri(context, sourceUri)
            var filename = sourceFile?.name ?: "Screenshot_${System.currentTimeMillis()}.png"
            if (!filename.startsWith("Tempo_")) {
                filename = "Tempo_$filename"
            }

            val relativePath = if (subfolder != null) {
                "${Environment.DIRECTORY_DCIM}/Temporatic/$subfolder"
            } else {
                "${Environment.DIRECTORY_DCIM}/Temporatic"
            }

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = context.contentResolver.insert(collection, values) ?: return null

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }

            return uri
        } catch (e: Exception) {
            Log.e("StorageManager", "Failed to copy screenshot to MediaStore", e)
        }
        return null
    }

    fun deleteFile(uri: Uri) {
        try {
            val file = DocumentFile.fromSingleUri(context, uri)
            if (file != null && file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Failed to delete file: $uri", e)
        }
    }

    fun deleteAppFolder(appLabel: String) {
        val rootFolderUri = getSavedFolderUri() ?: return
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, rootFolderUri)
            val appFolder = rootDoc?.findFile(appLabel)
            if (appFolder != null && appFolder.isDirectory) {
                appFolder.delete()
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Failed to delete folder: $appLabel", e)
        }
    }

    fun deleteAllFilesAndFolders() {
        val rootFolderUri = getSavedFolderUri() ?: return
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, rootFolderUri)
            rootDoc?.listFiles()?.forEach { 
                if (it.exists()) {
                    it.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Failed to delete all files and folders", e)
        }
    }

    private fun getOrCreateSubfolder(parent: DocumentFile, name: String): DocumentFile {
        val existing = parent.findFile(name)
        if (existing != null && existing.isDirectory) return existing
        return parent.createDirectory(name) ?: parent
    }
}