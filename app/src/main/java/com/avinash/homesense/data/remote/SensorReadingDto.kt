package com.avinash.homesense.data.remote

import com.avinash.homesense.data.model.Reading
import java.time.OffsetDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SensorReadingDto(
    val id: Long,
    @SerialName("created_at") val createdAt: String,
    val temperature: Float,
    val humidity: Float,
)

fun SensorReadingDto.toDomain(): Reading = Reading(
    timestamp = OffsetDateTime.parse(createdAt).toInstant(),
    temperatureCelsius = temperature.toDouble(),
    humidityPercent = humidity.toDouble(),
)
