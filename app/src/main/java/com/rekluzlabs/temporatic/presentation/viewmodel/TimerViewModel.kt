package com.rekluzlabs.temporatic.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor() : ViewModel() {
    
    private val _currentScreen = MutableStateFlow("home")
    val currentScreen = _currentScreen.asStateFlow()

    private val _timerSeconds = MutableStateFlow(5)
    val timerSeconds = _timerSeconds.asStateFlow()

    private val _countdownState = MutableStateFlow(0)
    val countdownState = _countdownState.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap = _capturedBitmap.asStateFlow()

    private var countdownJob: Job? = null

    fun setTimerSeconds(seconds: Int) {
        _timerSeconds.value = seconds
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun startCountdown(duration: Int) {
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
            // Logic to trigger capture will go here in Day 3
            // For now, just a placeholder navigation
            delay(500)
            _currentScreen.value = "preview"
        }
    }

    fun reset() {
        countdownJob?.cancel()
        _capturedBitmap.value = null
        _currentScreen.value = "home"
    }
}
