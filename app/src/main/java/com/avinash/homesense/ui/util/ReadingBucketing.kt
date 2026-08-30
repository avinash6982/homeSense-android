package com.avinash.homesense.ui.util

import com.avinash.homesense.data.model.Bucket
import com.avinash.homesense.data.model.BucketStat
import com.avinash.homesense.data.model.Reading
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Groups raw readings into fixed-width buckets spanning [date] (local time).
 * A bucket with no readings in it stays null so the chart can render a gap
 * instead of fabricating a value. For "today", only buckets up to and
 * including the one containing [now] are generated — later buckets don't
 * exist yet — and that final bucket is marked [Bucket.isPartial] since it's
 * still accumulating readings.
 */
fun bucketReadings(
    readings: List<Reading>,
    date: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
    bucketMinutes: Int = 30,
    now: Instant = Instant.now(),
): List<Bucket> {
    val dayStart = date.atStartOfDay(zone).toInstant()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
    val bucketDurationSeconds = bucketMinutes * 60L
    val bucketCount = (24 * 60 / bucketMinutes)

    fun indexOf(instant: Instant): Int =
        (Duration.between(dayStart, instant).seconds / bucketDurationSeconds)
            .toInt()
            .coerceIn(0, bucketCount - 1)

    val nowIsWithinDay = !now.isBefore(dayStart) && now.isBefore(dayEnd)
    val partialIndex = if (nowIsWithinDay) indexOf(now) else null
    val lastIndex = when {
        nowIsWithinDay -> partialIndex!!
        now.isBefore(dayStart) -> return emptyList() // date is in the future — nothing to show
        else -> bucketCount - 1 // a past day: all buckets are settled
    }

    val grouped = readings
        .filter { !it.timestamp.isBefore(dayStart) && it.timestamp.isBefore(dayEnd) }
        .groupBy { indexOf(it.timestamp) }

    return (0..lastIndex).map { index ->
        val bucketStart = dayStart.plusSeconds(index * bucketDurationSeconds)
        val bucketReadings = grouped[index].orEmpty()
        Bucket(
            start = bucketStart,
            end = bucketStart.plusSeconds(bucketDurationSeconds),
            temperature = bucketReadings.statOf { it.temperatureCelsius },
            humidity = bucketReadings.statOf { it.humidityPercent },
            isPartial = index == partialIndex,
            rawReadings = bucketReadings,
        )
    }
}

private fun List<Reading>.statOf(valueOf: (Reading) -> Double): BucketStat? {
    if (isEmpty()) return null
    val values = map(valueOf)
    return BucketStat(mean = values.average(), min = values.min(), max = values.max(), count = size)
}
