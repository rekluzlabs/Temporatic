package com.rekluzlabs.temporatic.domain

import android.net.Uri

sealed class ScreenshotEvent {
    data class SystemScreenshotDetected(
        val fileUri: Uri,
        val timestamp: Long,
        val filename: String
    ) : ScreenshotEvent()
}
