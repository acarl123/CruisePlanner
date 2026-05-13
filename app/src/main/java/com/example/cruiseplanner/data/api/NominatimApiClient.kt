package com.example.cruiseplanner.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton client for Nominatim geocoding API.
 * Nominatim is OpenStreetMap's free geocoding service - no API key required.
 */
object NominatimApiClient {

    /**
     * Nominatim public server.
     * Usage policy: https://operations.osmfoundation.org/policies/nominatim/
     * - Limit to 1 request per second
     * - Include a valid User-Agent header
     */
    private const val BASE_URL = "https://nominatim.openstreetmap.org/"

    /**
     * Create OkHttpClient with logging and timeout configuration.
     */
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                // Add User-Agent header as required by Nominatim usage policy
                val request = chain.request().newBuilder()
                    .header("User-Agent", "CruisePlanner Android App")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Lazy-initialized Retrofit instance.
     */
    private val retrofitInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Get the Nominatim API service instance.
     */
    val service: NominatimApiService by lazy {
        retrofitInstance.create(NominatimApiService::class.java)
    }
}
