package com.avinash.homesense.data.repository

import com.avinash.homesense.BuildConfig
import com.avinash.homesense.data.remote.SupabaseApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Manual DI holder. Uses [SupabaseClimateRepository] once a Supabase key is
 * configured in local.properties; falls back to [FakeClimateRepository] so
 * the app still runs (with mock data) before the key is set up.
 */
object RepositoryProvider {

    val repository: ClimateRepository by lazy {
        if (BuildConfig.SUPABASE_API_KEY.isBlank()) {
            FakeClimateRepository()
        } else {
            SupabaseClimateRepository(api)
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_API_KEY}")
                    .build()
                chain.proceed(request)
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("${BuildConfig.SUPABASE_URL}/rest/v1/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val api: SupabaseApi by lazy { retrofit.create(SupabaseApi::class.java) }
}
