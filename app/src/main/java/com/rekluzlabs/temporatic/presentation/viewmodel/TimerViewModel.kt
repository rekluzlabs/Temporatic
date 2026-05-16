package com.rekluzlabs.temporatic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor() : ViewModel() {
    private val _timerSeconds = MutableStateFlow(5)
    val timerSeconds = _timerSeconds.asStateFlow()
    
    fun setTimerSeconds(seconds: Int) {
        _timerSeconds.value = seconds
    }
}
