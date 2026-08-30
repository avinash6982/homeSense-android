package com.avinash.homesense.data.repository

import com.avinash.homesense.data.model.ClimateResult
import com.avinash.homesense.data.model.Reading
import java.time.LocalDate

/** Abstraction over the climate data source backed by the Supabase sensor_readings table. */
interface ClimateRepository {

    /** One-shot fetch of the latest reading. Callers own polling/refresh timing. */
    suspend fun fetchCurrentReading(): ClimateResult<Reading>

    /** Readings for a single calendar day (local time), chronologically ordered. */
    suspend fun getReadingsForDay(date: LocalDate): ClimateResult<List<Reading>>
}
