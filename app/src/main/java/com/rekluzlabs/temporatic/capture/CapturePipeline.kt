package com.rekluzlabs.temporatic.capture

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaActionSound
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.rekluzlabs.temporatic.data.ScreenshotRecord
import com.rekluzlabs.temporatic.data.ScreenshotRepository
import com.rekluzlabs.temporatic.manager.StorageManager
import com.rekluzlabs.temporatic.utils.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class CaptureFeedback(
    val playSound: Boolean = false,
    val vibrate: Boolean = true,
    val vibrationIntensity: Int = 128
)

@Singleton
class CapturePipeline @Inject constructor(
    private val context: Context,
    private val captureEngine: ScreenCaptureEngine,
    private val storageManager: StorageManager,
    private val repository: ScreenshotRepository
) {
    private val captureSound = MediaActionSound().apply {
        load(MediaActionSound.SHUTTER_CLICK)
    }

    suspend fun execute(
        request: CaptureRequest,
        feedback: CaptureFeedback = CaptureFeedback()
    ): CaptureResult = withContext(Dispatchers.IO) {
        Log.i("CapturePipeline", "Starting capture")

        if (feedback.playSound) captureSound.play(MediaActionSound.SHUTTER_CLICK)

        val bitmap = captureEngine.capture(request.projection)
            ?: return@withContext CaptureResult.Failure("capture returned null")

        var processed: Bitmap = bitmap

        if (request.watermarkEnabled) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())
            val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
            processed = BitmapUtils.addTimestampOverlay(processed, timestamp, deviceInfo)
        }

        if (request.resizeScale < 1.0f) {
            processed = BitmapUtils.resizeBitmap(processed, request.resizeScale)
        }

        if (feedback.vibrate) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, feedback.vibrationIntensity))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        }

        val subfolder = if (request.organizeByApp && request.appLabel != null)
            request.appLabel else null

        val uri = storageManager.saveScreenshot(processed, subfolder)
        if (uri == null) {
            return@withContext CaptureResult.Failure("saveScreenshot returned null")
        }

        val savedFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)
        val record = ScreenshotRecord(
            appLabel = request.appLabel,
            timestamp = System.currentTimeMillis(),
            filePath = uri.toString(),
            filename = savedFile?.name ?: "Tempo_${System.currentTimeMillis()}.png"
        )

        try {
            repository.insert(record)
        } catch (e: Exception) {
            Log.e("CapturePipeline", "DB insert failed", e)
        }

        Log.i("CapturePipeline", "Capture complete: $uri")

        CaptureResult.Success(bitmap = processed, uri = uri, record = record)
    }
}
