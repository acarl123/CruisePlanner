package com.example.cruiseplanner.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton client for OSRM routing API.
 * OSRM is completely free and doesn't require an API key.
 */
object OsrmApiClient {

    /**
     * Public OSRM demo server (free to use, but can be slow).
     * For production, consider self-hosting OSRM.
     */
    private const val PUBLIC_BASE_URL = "https://router.project-osrm.org/"

    /**
     * Current base URL.
     */
    private var baseUrl: String = PUBLIC_BASE_URL

    /**
     * Configure the client for self-hosted OSRM instance.
     *
     * @param url Base URL of the self-hosted instance (e.g., "http://192.168.1.100:5000/")
     */
    fun useSelfHosted(url: String) {
        baseUrl = url
        recreateService()
    }

    /**
     * Configure the client for public OSRM demo server.
     */
    fun usePublic() {
        baseUrl = PUBLIC_BASE_URL
        recreateService()
    }

    /**
     * Create OkHttpClient with logging and timeout configuration.
     */
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Lazy-initialized Retrofit instance.
     */
    private var retrofitInstance: Retrofit? = null
    private var currentBaseUrl: String = baseUrl

    private fun getRetrofit(): Retrofit {
        // Recreate if base URL changed
        if (retrofitInstance == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            retrofitInstance = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitInstance!!
    }

    /**
     * Get the OSRM API service instance.
     */
    val service: OsrmApiService by lazy {
        getRetrofit().create(OsrmApiService::class.java)
    }

    /**
     * Force recreation of the service.
     */
    fun recreateService() {
        retrofitInstance = null
    }
}
