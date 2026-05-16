package com.rekluzlabs.temporatic.domain.model

import android.net.Uri

data class ScreenshotData(
    val uri: Uri,
    val timestamp: Long,
    val name: String
)
