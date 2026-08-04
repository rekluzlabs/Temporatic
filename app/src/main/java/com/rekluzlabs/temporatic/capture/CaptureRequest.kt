package com.rekluzlabs.temporatic.capture

import android.media.projection.MediaProjection

data class CaptureRequest(
    val projection: MediaProjection,
    val watermarkEnabled: Boolean = true,
    val resizeScale: Float = 1.0f,
    val organizeByApp: Boolean = false,
    val foregroundPackage: String? = null,
    val appLabel: String? = null
)
