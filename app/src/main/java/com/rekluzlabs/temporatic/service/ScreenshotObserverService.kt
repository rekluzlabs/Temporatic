package com.rekluzlabs.temporatic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.rekluzlabs.temporatic.R
import com.rekluzlabs.temporatic.detection.ScreenshotContentObserver
import com.rekluzlabs.temporatic.domain.ScreenshotEvent
import com.rekluzlabs.temporatic.event.TemporaticEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScreenshotObserverService : Service() {

    @Inject lateinit var eventBus: TemporaticEventBus
    
    private var contentObserver: ContentObserver? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "temporatic_observer_channel"
        
        private var instance: ScreenshotObserverService? = null
        fun isRunning(): Boolean = instance != null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i("ScreenshotObserver", "Service onCreate")

        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        registerMediaStoreObserver()
    }

    private fun registerMediaStoreObserver() {
        contentObserver = ScreenshotContentObserver(
            Handler(Looper.getMainLooper())
        ) { _ ->
            onScreenshotDetected()
        }
        
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )
        
        Log.d("ScreenshotObserver", "MediaStore observer registered")
    }

    private var lastProcessedFilename: String? = null
    private var lastProcessTime: Long = 0

    private fun onScreenshotDetected() {
        scope.launch {
            // Guard against rapid triggers
            val nowTime = System.currentTimeMillis()
            if (nowTime - lastProcessTime < 2000) return@launch
            
            // Wait a bit for the system to finish writing the file
            kotlinx.coroutines.delay(1500)
            
            try {
                // Query for latest screenshot.
                val projection = mutableListOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        add(MediaStore.Images.Media.OWNER_PACKAGE_NAME)
                    }
                }.toTypedArray()

                val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val queryArgs = Bundle().apply {
                        // Exclude our own prefix in the query itself if possible, 
                        // but NOT all providers support complex LIKE. 
                        // We'll filter in code for safety.
                        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "(${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Images.Media.DATA} LIKE ?) AND ${MediaStore.Images.Media.DISPLAY_NAME} NOT LIKE ?")
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("%Screenshot%", "%Screenshots%", "Tempo_%"))
                        putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_ADDED))
                        putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                        putInt(ContentResolver.QUERY_ARG_LIMIT, 5) // Check a few latest ones
                    }
                    contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, queryArgs, null)
                } else {
                    @Suppress("DEPRECATION")
                    contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        "(${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Images.Media.DATA} LIKE ?) AND ${MediaStore.Images.Media.DISPLAY_NAME} NOT LIKE ?",
                        arrayOf("%Screenshot%", "%Screenshots%", "Tempo_%"),
                        "${MediaStore.Images.Media.DATE_ADDED} DESC"
                    )
                }

                cursor?.use {
                    while (it.moveToNext()) {
                        val displayName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                        val dateAdded = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                        val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                        val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))

                        // Double check the prefix in case SQL NOT LIKE failed
                        if (displayName.startsWith("Tempo_")) continue

                        // Ignore if we just processed this one
                        if (displayName == lastProcessedFilename) continue

                        // Ignore files created by our own app to prevent infinite loops
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val owner = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.OWNER_PACKAGE_NAME))
                            if (owner == packageName) {
                                Log.d("ScreenshotObserver", "Ignoring file created by our own app: $displayName")
                                continue
                            }
                        } else if (path.contains(packageName) || path.contains("Temporatic")) {
                             Log.d("ScreenshotObserver", "Ignoring file likely created by our own app: $path")
                             continue
                        }
                        
                        // Only process if it's actually a recent screenshot (within last 30 seconds)
                        val now = System.currentTimeMillis() / 1000
                        if (now - dateAdded > 30) {
                            Log.d("ScreenshotObserver", "Found old image, skipping: $displayName (age: ${now - dateAdded}s)")
                            continue
                        }

                        val fileUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                            .appendPath(id.toString())
                            .build()
                        
                        Log.d("ScreenshotObserver", "New screenshot detected: $displayName at $path")
                        
                        lastProcessedFilename = displayName
                        lastProcessTime = System.currentTimeMillis()

                        eventBus.emit(
                            ScreenshotEvent.SystemScreenshotDetected(
                                fileUri = fileUri,
                                timestamp = dateAdded * 1000,
                                filename = displayName
                            )
                        )
                        break // Only process the single latest valid screenshot
                    }
                }
            } catch (e: Exception) {
                Log.e("ScreenshotObserver", "Error detecting screenshot", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screenshot Observer Service",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Temporatic Observer Active")
            .setContentText("Organizing your screenshots")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        contentObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Log.e("ScreenshotObserver", "Error unregistering observer", e)
            }
        }
        Log.i("ScreenshotObserver", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
