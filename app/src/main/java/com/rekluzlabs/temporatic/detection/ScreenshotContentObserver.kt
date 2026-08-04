package com.rekluzlabs.temporatic.detection

import android.database.ContentObserver
import android.os.Handler
import android.util.Log

class ScreenshotContentObserver(handler: Handler, private val onScreenshotDetected: (String) -> Unit) : ContentObserver(handler) {
    
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        Log.d("ScreenshotObserver", "MediaStore change detected")
        onScreenshotDetected("screenshot_detected")
    }
}
