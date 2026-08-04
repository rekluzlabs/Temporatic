package com.rekluzlabs.temporatic.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "screenshots")
data class ScreenshotRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val filePath: String,
    val filename: String,
    val appLabel: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
