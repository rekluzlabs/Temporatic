package com.rekluzlabs.temporatic.manager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.rekluzlabs.temporatic.R

class FloatingButtonManager(private val context: Context, private val onClick: () -> Unit) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var lastClickTime = 0L

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (floatingView != null) return
        
        val prefs = context.getSharedPreferences("temporatic", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("floating_button_enabled", true)
        if (!isEnabled) return

        lastClickTime = 0L
        floatingView = LayoutInflater.from(context).inflate(R.layout.layout_floating_button, null)
        
        val sizeType = prefs.getInt("floating_button_size", 2)
        val density = context.resources.displayMetrics.density
        val baseSize = when(sizeType) {
            1 -> 48
            2 -> 64
            3 -> 80
            else -> 64
        }
        val buttonSizePx = (baseSize * density).toInt()

        val transparency = prefs.getFloat("floating_button_transparency", 1.0f)
        val isLocked = prefs.getBoolean("floating_button_locked", false)
        val buttonColor = prefs.getInt("floating_button_color", 0xFF6200EE.toInt())

        val btn = floatingView?.findViewById<ImageView>(R.id.btn_floating)
        btn?.layoutParams?.width = buttonSizePx
        btn?.layoutParams?.height = buttonSizePx
        btn?.alpha = transparency
        btn?.background?.setTint(buttonColor)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 16
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        floatingView?.let { view ->
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isMoving = false

            view.setOnTouchListener { v, event ->
                if (isLocked) {
                    if (event.action == MotionEvent.ACTION_UP) {
                        onClick()
                    }
                    return@setOnTouchListener true
                }

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params!!.x
                        initialY = params!!.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoving = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isMoving = true
                        }
                        params!!.x = initialX + dx
                        params!!.y = initialY + dy
                        windowManager.updateViewLayout(view, params)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoving) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClickTime >= 3000) {
                                lastClickTime = currentTime
                                onClick()
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            windowManager.addView(view, params)
        }
    }

    fun hide() {
        floatingView?.visibility = View.GONE
    }

    fun restore() {
        floatingView?.visibility = View.VISIBLE
    }

    fun destroy() {
        floatingView?.let {
            windowManager.removeView(it)
            floatingView = null
        }
    }
}
