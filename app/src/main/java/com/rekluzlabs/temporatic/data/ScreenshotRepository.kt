package com.rekluzlabs.temporatic.data

import android.content.Context
import android.net.Uri
import com.rekluzlabs.temporatic.manager.StorageManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenshotRepository @Inject constructor(
    private val context: Context,
    private val storageManager: StorageManager
) {
    private val dao = ScreenshotDatabase.getInstance(context).screenshotDao()

    suspend fun insert(record: ScreenshotRecord) = dao.insert(record)
    
    suspend fun delete(record: ScreenshotRecord) {
        storageManager.deleteFile(Uri.parse(record.filePath))
        dao.delete(record)
    }

    fun getRecent(limit: Int = 50): Flow<List<ScreenshotRecord>> = dao.getRecent(limit)
    fun getByApp(appLabel: String): Flow<List<ScreenshotRecord>> = dao.getByApp(appLabel)
    fun getAllApps(): Flow<List<String>> = dao.getAllApps()
    fun countByApp(appLabel: String): Flow<Int> = dao.countByApp(appLabel)

    suspend fun updateTags(id: String, tags: String) = dao.updateTags(id, tags)

    suspend fun deleteById(id: String) {
        deleteByIds(setOf(id))
    }

    suspend fun deleteByIds(ids: Set<String>) {
        val paths = dao.getPathsByIds(ids.toList())
        paths.forEach { path ->
            storageManager.deleteFile(Uri.parse(path))
        }
        dao.deleteByIds(ids.toList())
    }

    suspend fun deleteAll() {
        storageManager.deleteAllFilesAndFolders()
        dao.deleteAll()
    }

    suspend fun deleteByApp(appLabel: String) {
        storageManager.deleteAppFolder(appLabel)
        dao.deleteByApp(appLabel)
    }

    fun totalCount(): Flow<Int> = dao.totalCount()
}
