package com.rekluzlabs.temporatic.domain.model

data class TimerConfig(
    val durationSeconds: Int,
    val includeTimestamp: Boolean = true
)
