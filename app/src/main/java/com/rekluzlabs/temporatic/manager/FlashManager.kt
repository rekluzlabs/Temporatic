package com.rekluzlabs.temporatic.manager

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager

import android.provider.Settings
import android.util.Log

class FlashManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    fun flash(durationMs: Long = 100) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w("FlashManager", "Cannot flash: Overlay permission missing")
            return
        }

        val flashView = View(context).apply {
            setBackgroundColor(Color.WHITE)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        mainHandler.post {
            try {
                windowManager.addView(flashView, params)
                mainHandler.postDelayed({
                    try {
                        windowManager.removeView(flashView)
                    } catch (e: Exception) {
                        Log.e("FlashManager", "Error removing flash view", e)
                    }
                }, durationMs)
            } catch (e: Exception) {
                Log.e("FlashManager", "Error adding flash view. This often happens if overlay permission is revoked.", e)
            }
        }
    }
}
