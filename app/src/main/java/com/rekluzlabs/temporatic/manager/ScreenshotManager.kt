package com.rekluzlabs.temporatic.manager

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
import kotlinx.coroutines.*

class ScreenshotManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    fun captureScreenshot(mediaProjection: MediaProjection, callback: (Bitmap?) -> Unit) {
        Log.i("ScreenshotManager", "captureScreenshot triggered")
        scope.launch {
            try {
                val bitmap = captureScreen(mediaProjection)
                Log.i("ScreenshotManager", "captureScreen result: ${bitmap != null}")
                withContext(Dispatchers.Main) {
                    callback(bitmap)
                }
            } catch (e: Exception) {
                Log.e("ScreenshotManager", "Crash in captureScreenshot launch", e)
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    private suspend fun captureScreen(mediaProjection: MediaProjection): Bitmap? = withContext(Dispatchers.IO) {
        Log.d("ScreenshotManager", "captureScreen: getting display metrics")
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        Log.i("ScreenshotManager", "Display size: ${width}x$height @ ${density}dpi")

        if (width <= 0 || height <= 0 || density <= 0) {
            Log.e("ScreenshotManager", "Invalid display metrics: ${width}x$height @ ${density}dpi")
            return@withContext null
        }

        Log.d("ScreenshotManager", "captureScreen: creating ImageReader")
        // Use a dedicated HandlerThread so the ImageReader listener fires off the main thread
        val handlerThread = android.os.HandlerThread("ScreenshotCapture").also { it.start() }
        val handler = android.os.Handler(handlerThread.looper)
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        Log.d("ScreenshotManager", "captureScreen: creating VirtualDisplay")
        val virtualDisplay = try {
            mediaProjection.createVirtualDisplay(
                "TemporaticCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface, null, null
            )
        } catch (e: Exception) {
            Log.e("ScreenshotManager", "createVirtualDisplay threw", e)
            imageReader.close()
            handlerThread.quitSafely()
            return@withContext null
        }
        Log.d("ScreenshotManager", "captureScreen: VirtualDisplay created, is null? ${virtualDisplay == null}")

        if (virtualDisplay == null) {
            Log.e("ScreenshotManager", "createVirtualDisplay returned null")
            imageReader.close()
            handlerThread.quitSafely()
            return@withContext null
        }

        // Use a CompletableDeferred so we return the instant the first frame is available
        // instead of sleeping for a fixed 1000ms regardless of frame arrival time.
        val frameDeferred = CompletableDeferred<Bitmap?>()

        imageReader.setOnImageAvailableListener({ reader ->
            if (frameDeferred.isCompleted) return@setOnImageAvailableListener
            var bitmap: Bitmap? = null
            try {
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                Log.d("ScreenshotManager", "acquireLatestImage returned frame")
                try {
                    val planes = image.planes
                    Log.d("ScreenshotManager", "planes count: ${planes.size}")
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width
                    Log.d("ScreenshotManager", "pixelStride=$pixelStride rowStride=$rowStride rowPadding=$rowPadding")

                    bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    buffer.rewind()
                    bitmap.copyPixelsFromBuffer(buffer)

                    if (rowPadding > 0) {
                        Log.d("ScreenshotManager", "cropping bitmap to remove row padding")
                        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                        bitmap.recycle()
                        bitmap = croppedBitmap
                    }
                } finally {
                    image.close()
                }
            } catch (e: Exception) {
                Log.e("ScreenshotManager", "Error acquiring/processing image in listener", e)
            }
            frameDeferred.complete(bitmap)
        }, handler)

        // 2-second timeout safety net — if the display never renders a frame, don't hang forever
        val bitmap = withTimeoutOrNull(2000L) { frameDeferred.await() }

        Log.d("ScreenshotManager", "releasing VirtualDisplay and ImageReader")
        try { virtualDisplay.release() } catch (_: Exception) {}
        try { imageReader.close() } catch (_: Exception) {}
        try { handlerThread.quitSafely() } catch (_: Exception) {}

        bitmap
    }
}
