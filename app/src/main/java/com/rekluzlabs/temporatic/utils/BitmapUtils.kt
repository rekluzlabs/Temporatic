package com.rekluzlabs.temporatic.utils

import android.graphics.*

object BitmapUtils {
    fun addTimestampOverlay(
        bitmap: Bitmap,
        timestamp: String,
        deviceInfo: String
    ): Bitmap {
        val barHeight = 160
        val result = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height + barHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        
        // Draw original screenshot
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        
        // Draw dark bar
        val barPaint = Paint().apply {
            color = Color.argb(240, 10, 10, 12)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, bitmap.height.toFloat(), bitmap.width.toFloat(), (bitmap.height + barHeight).toFloat(), barPaint)
        
        // Draw divider
        val dividerPaint = Paint().apply {
            color = Color.argb(100, 59, 130, 246) // PrimaryBlue with alpha
            strokeWidth = 4f
        }
        canvas.drawLine(0f, bitmap.height.toFloat(), bitmap.width.toFloat(), bitmap.height.toFloat(), dividerPaint)

        // Draw text
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val margin = 40f
        canvas.drawText("TEMPORATIC PROOF", margin, bitmap.height + 60f, textPaint)
        
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textPaint.textSize = 36f
        textPaint.color = Color.argb(200, 255, 255, 255)
        
        canvas.drawText("Timestamp: $timestamp", margin, bitmap.height + 105f, textPaint)
        canvas.drawText("Device: $deviceInfo", margin, bitmap.height + 145f, textPaint)
        
        return result
    }
}
