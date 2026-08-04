package com.rekluzlabs.temporatic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.rekluzlabs.temporatic.data.ScreenshotRecord
import com.rekluzlabs.temporatic.data.ScreenshotRepository
import com.rekluzlabs.temporatic.manager.*
import com.rekluzlabs.temporatic.processing.AppNameResolver
import com.rekluzlabs.temporatic.processing.ForegroundAppTracker
import com.rekluzlabs.temporatic.ui.CapturePreviewManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class TemporaticService : Service() {

    @Inject lateinit var screenshotMgr: ScreenshotManager
    @Inject lateinit var storageMgr: StorageManager
    @Inject lateinit var flashMgr: FlashManager
    @Inject lateinit var appNameResolver: AppNameResolver
    @Inject lateinit var foregroundAppTracker: ForegroundAppTracker
    @Inject lateinit var repository: ScreenshotRepository
    @Inject lateinit var capturePreviewManager: CapturePreviewManager
    private lateinit var floatingButtonMgr: FloatingButtonManager
    private var mediaProjection: MediaProjection? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val isCapturing = AtomicBoolean(false)
    private var lastCaptureTime = 0L
    
    private val captureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleCapture()
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "temporatic_channel"

        private var instance: TemporaticService? = null
        private val captureSound = MediaActionSound().apply {
            load(MediaActionSound.SHUTTER_CLICK)
        }

        fun isRunning(): Boolean = instance != null
        fun hasActiveProjection(): Boolean = instance?.mediaProjection != null
        fun requestCapture() {
            instance?.handleCapture()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i("TemporaticService", "Service onCreate")

        floatingButtonMgr = FloatingButtonManager(this) {
            handleCapture()
        }

        createNotificationChannel()
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val filter = IntentFilter("com.rekluzlabs.temporatic.CAPTURE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(captureReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(captureReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.e("TemporaticService", "Service restarted with null intent - mediaProjection lost")
            Toast.makeText(this, "Session expired. Re-open app to restart Temporatic Live Capture.", Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent.action == "com.rekluzlabs.temporatic.CAPTURE_ACTION") {
            handleCapture()
            return START_STICKY
        }

        val resultCode = intent.getIntExtra("resultCode", 0)
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("resultData", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Intent>("resultData")
        }

        if (resultCode != 0 && resultData != null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            try {
                val projection = projectionManager.getMediaProjection(resultCode, resultData)
                if (projection != null) {
                    mediaProjection = projection
                    projection.registerCallback(object : MediaProjection.Callback() {
                        override fun onStop() {
                            Log.w("TemporaticService", "MediaProjection stopped by system")
                            mediaProjection = null
                            floatingButtonMgr.destroy()
                            Toast.makeText(this@TemporaticService, "Screen capture permission revoked by system", Toast.LENGTH_LONG).show()
                            stopSelf()
                        }
                    }, null)
                    floatingButtonMgr.show()
                    Toast.makeText(this, "Temporatic Live Capture is active", Toast.LENGTH_SHORT).show()
                    Log.d("TemporaticService", "MediaProjection acquired successfully")
                } else {
                    Log.e("TemporaticService", "getMediaProjection returned null")
                    Toast.makeText(this, "Failed to initialize media projection", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("TemporaticService", "getMediaProjection threw exception on Android ${Build.VERSION.SDK_INT}", e)
                Toast.makeText(this, "Permission error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        return START_STICKY
    }

    private fun playCaptureFeedback() {
        val prefs = getSharedPreferences("temporatic", MODE_PRIVATE)
        if (prefs.getBoolean("capture_sound_enabled", false)) {
            captureSound.play(MediaActionSound.SHUTTER_CLICK)
        }
        if (prefs.getBoolean("vibrate_enabled", true)) {
            val intensity = prefs.getInt("vibration_intensity", 128).coerceIn(0, 255)
            if (intensity > 0) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, intensity))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        }
    }

    fun handleCapture() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCaptureTime < 3000) {
            Log.d("TemporaticService", "Debouncing capture request: ${3000 - (currentTime - lastCaptureTime)}ms remaining")
            return
        }

        if (isCapturing.getAndSet(true)) {
            Log.w("TemporaticService", "Capture already in progress, skipping")
            return
        }

        lastCaptureTime = currentTime

        val projection = mediaProjection
        if (projection == null) {
            Log.e("TemporaticService", "handleCapture: mediaProjection is NULL")
            isCapturing.set(false)
            flashMgr.flash()
            Toast.makeText(this, "Session expired. Re-open app to restart Temporatic Live Capture.", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }

        Log.i("TemporaticService", "Starting capture process")

        playCaptureFeedback()
        floatingButtonMgr.hide()
        flashMgr.flash()

        val prefs = getSharedPreferences("temporatic", MODE_PRIVATE)
        val organizeByApp = prefs.getBoolean("organize_by_app", false)
        val foregroundPackage = if (organizeByApp) foregroundAppTracker.getForegroundApp() else null
        val appLabel = appNameResolver.getAppLabel(foregroundPackage)
        val subfolder = if (organizeByApp && appLabel != null) appLabel else null

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                screenshotMgr.captureScreenshot(projection) { bitmap ->
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            if (bitmap != null) {
                                var savedUri: Uri? = null
                                var savedFilename = ""
                                var savedRecordId = ""

                                try {
                                    val prefs = getSharedPreferences("temporatic", MODE_PRIVATE)
                                    val watermarkEnabled = prefs.getBoolean("watermark_enabled", true)
                                    val resizeScale = prefs.getFloat("resize_scale", 1.0f)

                                    var processed = bitmap
                                    
                                    if (watermarkEnabled) {
                                        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                        val deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                                        processed = com.rekluzlabs.temporatic.utils.BitmapUtils.addTimestampOverlay(
                                            processed,
                                            timestamp,
                                            deviceInfo
                                        )
                                    }

                                    if (resizeScale < 1.0f) {
                                        processed = com.rekluzlabs.temporatic.utils.BitmapUtils.resizeBitmap(processed, resizeScale)
                                    }

                                    val uri = storageMgr.saveScreenshot(processed, subfolder)
                                    Log.d("TemporaticService", "saveScreenshot returned uri=$uri")
                                    if (uri != null) {
                                        val savedFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(this@TemporaticService, uri)
                                        val record = ScreenshotRecord(
                                            appLabel = appLabel,
                                            timestamp = System.currentTimeMillis(),
                                            filePath = uri.toString(),
                                            filename = savedFile?.name ?: "Tempo_${System.currentTimeMillis()}.png"
                                        )
                                        try {
                                            repository.insert(record)
                                        } catch (e: Exception) {
                                            Log.e("TemporaticService", "Failed to insert DB record", e)
                                        }
                                        savedUri = uri
                                        savedFilename = record.filename
                                        savedRecordId = record.id
                                    } else {
                                        Log.w("TemporaticService", "saveScreenshot returned null, skipping overlay")
                                    }
                                } catch (e: Exception) {
                                    Log.e("TemporaticService", "Bitmap processing failed, trying fallback", e)
                                    val fallbackUri = storageMgr.saveScreenshot(bitmap, subfolder)
                                    if (fallbackUri != null) {
                                        val savedFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(this@TemporaticService, fallbackUri)
                                        val record = ScreenshotRecord(
                                            appLabel = appLabel,
                                            timestamp = System.currentTimeMillis(),
                                            filePath = fallbackUri.toString(),
                                            filename = savedFile?.name ?: "Tempo_${System.currentTimeMillis()}.png"
                                        )
                                        try {
                                            repository.insert(record)
                                        } catch (e: Exception) {
                                            Log.e("TemporaticService", "Failed to insert fallback DB record", e)
                                        }
                                        savedUri = fallbackUri
                                        savedFilename = record.filename
                                        savedRecordId = record.id
                                    }
                                }

                                // Show overlay and toast from whichever path succeeded
                                val finalUri = savedUri
                                if (finalUri != null) {
                                    val prefs = getSharedPreferences("temporatic", MODE_PRIVATE)
                                    val showPreview = prefs.getBoolean("share_after_capture", true)
                                    Log.d("TemporaticService", "share_after_capture=$showPreview, savedUri=$finalUri")
                                    withContext(Dispatchers.Main) {
                                        if (showPreview) {
                                            capturePreviewManager.show(finalUri, savedFilename, savedRecordId)
                                        }
                                        Toast.makeText(this@TemporaticService, "Screenshot saved", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Log.e("TemporaticService", "Screenshot manager returned null")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@TemporaticService, "Screen capture failed. Try lowering display resolution or restarting the service.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TemporaticService", "Error during screenshot processing", e)
                        } finally {
                            withContext(Dispatchers.Main) {
                                floatingButtonMgr.restore()
                                isCapturing.set(false)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TemporaticService", "handleCapture failed", e)
                floatingButtonMgr.restore()
                isCapturing.set(false)
            }
        }, 200)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Temporatic Capture Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Temporatic Active")
            .setContentText("Tap the camera button to capture")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        try { unregisterReceiver(captureReceiver) } catch (e: Exception) {}
        floatingButtonMgr.destroy()
        mediaProjection?.stop()
        Log.i("TemporaticService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}