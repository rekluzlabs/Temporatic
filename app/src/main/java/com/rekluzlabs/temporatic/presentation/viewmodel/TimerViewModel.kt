package com.rekluzlabs.temporatic.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekluzlabs.temporatic.data.service.ScreenCaptureManager
import com.rekluzlabs.temporatic.utils.BitmapUtils
import com.rekluzlabs.temporatic.utils.DateTimeUtils
import com.rekluzlabs.temporatic.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val screenshotManager: ScreenCaptureManager
) : ViewModel() {
    
    private val _currentScreen = MutableStateFlow("home")
    val currentScreen = _currentScreen.asStateFlow()

    private val _timerSeconds = MutableStateFlow(5)
    val timerSeconds = _timerSeconds.asStateFlow()

    private val _countdownState = MutableStateFlow(0)
    val countdownState = _countdownState.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap = _capturedBitmap.asStateFlow()

    private val _screenshotFile = MutableStateFlow<File?>(null)
    val screenshotFile = _screenshotFile.asStateFlow()

    private val _mediaProjectionIntent = MutableStateFlow<Intent?>(null)
    val mediaProjectionIntent = _mediaProjectionIntent.asStateFlow()

    private var countdownJob: Job? = null

    fun setTimerSeconds(seconds: Int) {
        _timerSeconds.value = seconds
    }

    fun setMediaProjectionIntent(intent: Intent?) {
        _mediaProjectionIntent.value = intent
    }

    fun startCountdown(duration: Int) {
        if (_mediaProjectionIntent.value == null) return

        _countdownState.value = duration
        _currentScreen.value = "countdown"
        
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in duration downTo 0) {
                _countdownState.value = i
                if (i > 0) {
                    delay(1000L)
                }
            }
            
            delay(500) // Small grace period
            captureScreenshot()
        }
    }

    private suspend fun captureScreenshot() {
        val intent = _mediaProjectionIntent.value ?: return
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = projectionManager.getMediaProjection(-1, intent)
        
        if (mediaProjection != null) {
            val rawBitmap = screenshotManager.captureScreen(mediaProjection)
            if (rawBitmap != null) {
                val timestamp = DateTimeUtils.getCurrentTimestamp()
                val deviceInfo = DateTimeUtils.getDeviceInfo()
                val annotatedBitmap = BitmapUtils.addTimestampOverlay(rawBitmap, timestamp, deviceInfo)
                
                val file = FileUtils.saveBitmap(context, annotatedBitmap)
                
                _capturedBitmap.value = annotatedBitmap
                _screenshotFile.value = file
                _currentScreen.value = "preview"
            } else {
                _currentScreen.value = "home"
            }
            mediaProjection.stop()
        } else {
            _currentScreen.value = "home"
        }
    }

    fun reset() {
        countdownJob?.cancel()
        _capturedBitmap.value = null
        _screenshotFile.value = null
        _currentScreen.value = "home"
    }
}
