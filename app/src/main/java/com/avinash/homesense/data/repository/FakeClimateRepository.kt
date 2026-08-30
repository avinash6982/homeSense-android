package com.avinash.homesense.data.repository

import com.avinash.homesense.data.model.ClimateResult
import com.avinash.homesense.data.model.Reading
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Generates plausible room temperature/humidity readings via a bounded random
 * walk. Kept around as an offline/dev fallback now that
 * [SupabaseClimateRepository] is the default — useful for previews or
 * running without a Supabase key configured.
 */
class FakeClimateRepository : ClimateRepository {

    private var lastTemperature = 23.5
    private var lastHumidity = 48.0

    override suspend fun fetchCurrentReading(): ClimateResult<Reading> =
        ClimateResult.Success(nextReading())

    override suspend fun getReadingsForDay(date: LocalDate): ClimateResult<List<Reading>> {
        val today = LocalDate.now()
        if (date.isAfter(today)) return ClimateResult.Success(emptyList())

        val random = Random(date.toEpochDay())
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant()
        val now = Instant.now()

        var temperature = 22.0 + random.nextDouble(-2.0, 2.0)
        var humidity = 45.0 + random.nextDouble(-5.0, 5.0)

        val readings = buildList {
            for (i in 0 until 96) { // one point every 15 minutes
                val timestamp = dayStart.plus((i * 15).toLong(), ChronoUnit.MINUTES)
                if (timestamp.isAfter(now)) break
                temperature = walk(temperature, step = 0.3, lowerBound = 15.0, upperBound = 32.0, random = random)
                humidity = walk(humidity, step = 1.2, lowerBound = 25.0, upperBound = 75.0, random = random)
                add(Reading(timestamp, temperature, humidity))
            }
        }
        return ClimateResult.Success(readings)
    }

    private fun nextReading(): Reading {
        lastTemperature = walk(lastTemperature, step = 0.15, lowerBound = 18.0, upperBound = 30.0)
        lastHumidity = walk(lastHumidity, step = 0.6, lowerBound = 30.0, upperBound = 70.0)
        return Reading(
            timestamp = Instant.now(),
            temperatureCelsius = lastTemperature,
            humidityPercent = lastHumidity,
        )
    }

    private fun walk(
        current: Double,
        step: Double,
        lowerBound: Double,
        upperBound: Double,
        random: Random = Random,
    ): Double {
        val delta = random.nextDouble(-step, step)
        return max(lowerBound, min(upperBound, current + delta))
    }
}
