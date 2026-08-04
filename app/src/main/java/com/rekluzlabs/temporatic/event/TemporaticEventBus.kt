package com.rekluzlabs.temporatic.event

import com.rekluzlabs.temporatic.domain.ScreenshotEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemporaticEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<ScreenshotEvent>(replay = 0, extraBufferCapacity = 10)
    val events: SharedFlow<ScreenshotEvent> = _events

    suspend fun emit(event: ScreenshotEvent) {
        _events.emit(event)
    }
}
