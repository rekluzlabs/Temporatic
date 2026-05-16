package com.rekluzlabs.temporatic.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekluzlabs.temporatic.data.service.ScreenCaptureManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
        if (_mediaProjectionIntent.value == null) {
            // Permission needed - this should be handled by the UI
            return
        }

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
            
            // Countdown finished - capture screenshot
            captureScreenshot()
        }
    }

    private suspend fun captureScreenshot() {
        val intent = _mediaProjectionIntent.value ?: return
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = projectionManager.getMediaProjection(-1, intent)
        
        if (mediaProjection != null) {
            val bitmap = screenshotManager.captureScreen(mediaProjection)
            _capturedBitmap.value = bitmap
            _currentScreen.value = "preview"
            mediaProjection.stop()
        } else {
            _currentScreen.value = "home"
        }
    }

    fun reset() {
        countdownJob?.cancel()
        _capturedBitmap.value = null
        _currentScreen.value = "home"
    }
}
