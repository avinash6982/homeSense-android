package com.avinash.homesense.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Thin wrapper over the Supabase PostgREST endpoint for the sensor_readings
 * table. [createdAtGte] and [createdAtLt] both map to the same "created_at"
 * query key so PostgREST sees two range filters, e.g.
 * created_at=gte.X&created_at=lt.Y.
 */
interface SupabaseApi {

    @GET("sensor_readings")
    suspend fun getReadings(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("created_at") createdAtGte: String? = null,
        @Query("created_at") createdAtLt: String? = null,
    ): List<SensorReadingDto>
}
