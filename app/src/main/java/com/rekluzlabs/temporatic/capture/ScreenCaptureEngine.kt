package com.rekluzlabs.temporatic.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CompletableDeferred

class ScreenCaptureEngine(private val context: Context) {

    suspend fun capture(projection: MediaProjection): Bitmap? = withContext(Dispatchers.IO) {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val handlerThread = android.os.HandlerThread("CaptureEngine").also { it.start() }
        val handler = android.os.Handler(handlerThread.looper)
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        val virtualDisplay = try {
            projection.createVirtualDisplay(
                "TemporaticCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface, null, null
            )
        } catch (e: Exception) {
            Log.e("ScreenCaptureEngine", "createVirtualDisplay failed", e)
            imageReader.close(); handlerThread.quitSafely()
            return@withContext null
        }

        if (virtualDisplay == null) {
            imageReader.close(); handlerThread.quitSafely()
            return@withContext null
        }

        val frameDeferred = CompletableDeferred<Bitmap?>()

        imageReader.setOnImageAvailableListener({ reader ->
            if (frameDeferred.isCompleted) return@setOnImageAvailableListener
            var bitmap: Bitmap? = null
            try {
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height, Bitmap.Config.ARGB_8888
                    )
                    buffer.rewind()
                    bitmap.copyPixelsFromBuffer(buffer)

                    if (rowPadding > 0) {
                        val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                        bitmap.recycle()
                        bitmap = cropped
                    }
                } finally {
                    image.close()
                }
            } catch (e: Exception) {
                Log.e("ScreenCaptureEngine", "Image processing error", e)
            }
            frameDeferred.complete(bitmap)
        }, handler)

        val bitmap = withTimeoutOrNull(2000L) { frameDeferred.await() }

        virtualDisplay.release()
        imageReader.close()
        handlerThread.quitSafely()

        bitmap
    }
}
