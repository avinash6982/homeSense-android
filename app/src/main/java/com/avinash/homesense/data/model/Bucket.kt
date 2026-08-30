package com.avinash.homesense.data.model

import java.time.Instant

data class BucketStat(
    val mean: Double,
    val min: Double,
    val max: Double,
    val count: Int,
)

data class Bucket(
    val start: Instant,
    val end: Instant,
    val temperature: BucketStat?,
    val humidity: BucketStat?,
    val isPartial: Boolean,
    val rawReadings: List<Reading>,
)
