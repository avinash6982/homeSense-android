package com.avinash.homesense.data.model

import java.time.Instant

data class Reading(
    val timestamp: Instant,
    val temperatureCelsius: Double,
    val humidityPercent: Double,
)
