package com.rekluzlabs.temporatic.capture

import android.graphics.Bitmap
import android.net.Uri
import com.rekluzlabs.temporatic.data.ScreenshotRecord

sealed class CaptureResult {
    data class Success(
        val bitmap: Bitmap,
        val uri: Uri,
        val record: ScreenshotRecord
    ) : CaptureResult()

    data class Failure(
        val reason: String,
        val exception: Throwable? = null
    ) : CaptureResult()
}
