package com.rekluzlabs.temporatic.service

import android.util.Log
import com.rekluzlabs.temporatic.domain.ScreenshotEvent
import com.rekluzlabs.temporatic.event.TemporaticEventBus
import com.rekluzlabs.temporatic.processing.FileProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenshotProcessingOrchestrator @Inject constructor(
    private val eventBus: TemporaticEventBus,
    private val fileProcessor: FileProcessor,
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startListening() {
        Log.d("Orchestrator", "Starting to listen for screenshot events")

        scope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is ScreenshotEvent.SystemScreenshotDetected -> {
                        Log.d("Orchestrator", "Processing event: ${event.filename}")

                        try {
                            fileProcessor.processScreenshot(event)
                        } catch (e: Exception) {
                            Log.e("Orchestrator", "Error processing screenshot", e)
                        }
                    }
                }
            }
        }
    }
}
