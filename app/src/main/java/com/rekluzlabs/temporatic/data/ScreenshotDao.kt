package com.rekluzlabs.temporatic.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {
    @Insert suspend fun insert(record: ScreenshotRecord)
    @Delete suspend fun delete(record: ScreenshotRecord)

    @Query("SELECT * FROM screenshots ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<ScreenshotRecord>>

    @Query("SELECT * FROM screenshots WHERE appLabel = :appLabel ORDER BY timestamp DESC")
    fun getByApp(appLabel: String): Flow<List<ScreenshotRecord>>

    @Query("SELECT DISTINCT appLabel FROM screenshots WHERE appLabel IS NOT NULL ORDER BY appLabel")
    fun getAllApps(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM screenshots WHERE appLabel = :appLabel")
    fun countByApp(appLabel: String): Flow<Int>

    @Query("UPDATE screenshots SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: String, tags: String)

    @Query("DELETE FROM screenshots WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT filePath FROM screenshots WHERE id IN (:ids)")
    suspend fun getPathsByIds(ids: List<String>): List<String>

    @Query("DELETE FROM screenshots WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM screenshots")
    suspend fun deleteAll()

    @Query("DELETE FROM screenshots WHERE appLabel = :appLabel")
    suspend fun deleteByApp(appLabel: String)

    @Query("SELECT COUNT(*) FROM screenshots")
    fun totalCount(): Flow<Int>
}
