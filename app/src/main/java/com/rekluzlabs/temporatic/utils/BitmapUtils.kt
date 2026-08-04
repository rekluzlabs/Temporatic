package com.rekluzlabs.temporatic.utils

import android.graphics.*

object BitmapUtils {
    fun resizeBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        if (scale >= 1f) return bitmap
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    fun addTimestampOverlay(
        bitmap: Bitmap,
        timestamp: String,
        deviceInfo: String
    ): Bitmap {
        // Force conversion to software bitmap by making it mutable. Hardware bitmaps cannot be mutable.
        val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: bitmap
        
        // Only recycle if we successfully created a copy, so we don't recycle the only copy if it failed
        if (softwareBitmap !== bitmap) {
            bitmap.recycle()
        }

        val barHeight = 160
        val result = Bitmap.createBitmap(
            softwareBitmap.width,
            softwareBitmap.height + barHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)

        // Draw original screenshot (now safe with software bitmap)
        canvas.drawBitmap(softwareBitmap, 0f, 0f, null)

        // Draw dark bar
        val barPaint = Paint().apply {
            color = Color.argb(240, 10, 10, 12)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, softwareBitmap.height.toFloat(), softwareBitmap.width.toFloat(), (softwareBitmap.height + barHeight).toFloat(), barPaint)

        // Draw divider
        val dividerPaint = Paint().apply {
            color = Color.argb(100, 59, 130, 246) // PrimaryBlue with alpha
            strokeWidth = 4f
        }
        canvas.drawLine(0f, softwareBitmap.height.toFloat(), softwareBitmap.width.toFloat(), softwareBitmap.height.toFloat(), dividerPaint)

        // Draw text
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val margin = 40f
        canvas.drawText("TEMPORATIC PROOF", margin, softwareBitmap.height + 60f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textPaint.textSize = 36f
        textPaint.color = Color.argb(200, 255, 255, 255)

        canvas.drawText("Timestamp: $timestamp", margin, softwareBitmap.height + 105f, textPaint)
        canvas.drawText("Device: $deviceInfo", margin, softwareBitmap.height + 145f, textPaint)

        return result
    }
}