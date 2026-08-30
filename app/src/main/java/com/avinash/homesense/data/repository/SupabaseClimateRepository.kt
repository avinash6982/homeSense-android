package com.avinash.homesense.data.repository

import com.avinash.homesense.data.model.ClimateError
import com.avinash.homesense.data.model.ClimateResult
import com.avinash.homesense.data.model.Reading
import com.avinash.homesense.data.remote.SensorReadingDto
import com.avinash.homesense.data.remote.SupabaseApi
import com.avinash.homesense.data.remote.toDomain
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class SupabaseClimateRepository(
    private val api: SupabaseApi,
) : ClimateRepository {

    companion object {
        // The Supabase project caps every response at its configured "Max Rows"
        // (this project: 1000), regardless of a higher client-requested `limit` —
        // a full day at a ~30s sensor cadence is ~2,880 rows, so a single request
        // silently truncates to the first slice of the day. Page through with
        // `offset` instead, capped overall so one runaway day can't fetch forever.
        private const val PAGE_SIZE = 1000
        private const val MAX_READINGS_PER_DAY = 10_000
    }

    override suspend fun fetchCurrentReading(): ClimateResult<Reading> {
        val result = safeCall { api.getReadings(order = "created_at.desc", limit = 1) }
        return when (result) {
            is ClimateResult.Success -> {
                val reading = result.data.firstOrNull()?.toDomain()
                if (reading == null) ClimateResult.Failure(ClimateError.EMPTY) else ClimateResult.Success(reading)
            }
            is ClimateResult.Failure -> result
        }
    }

    override suspend fun getReadingsForDay(date: LocalDate): ClimateResult<List<Reading>> {
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()

        val result = safeCall {
            val all = mutableListOf<SensorReadingDto>()
            var offset = 0
            while (all.size < MAX_READINGS_PER_DAY) {
                val page = api.getReadings(
                    order = "created_at.asc",
                    limit = PAGE_SIZE,
                    offset = offset,
                    createdAtGte = "gte.${DateTimeFormatter.ISO_INSTANT.format(dayStart)}",
                    createdAtLt = "lt.${DateTimeFormatter.ISO_INSTANT.format(dayEnd)}",
                )
                all += page
                if (page.size < PAGE_SIZE) break
                offset += PAGE_SIZE
            }
            all
        }
        return when (result) {
            is ClimateResult.Success -> ClimateResult.Success(result.data.map { it.toDomain() }.sortedBy { it.timestamp })
            is ClimateResult.Failure -> result
        }
    }

    private suspend fun safeCall(block: suspend () -> List<SensorReadingDto>): ClimateResult<List<SensorReadingDto>> =
        try {
            ClimateResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ClimateResult.Failure(ClimateError.NO_INTERNET)
        } catch (e: HttpException) {
            ClimateResult.Failure(ClimateError.API_FAILURE)
        } catch (e: Exception) {
            ClimateResult.Failure(ClimateError.API_FAILURE)
        }
}
