package com.example.cruiseplanner.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton client for GraphHopper routing API.
 * Configurable for both cloud and self-hosted deployments.
 */
object GraphHopperApiClient {

    /**
     * Base URLs for different deployment types.
     * Default: Cloud API (free tier, rate limited)
     * Self-hosted: Configure with your own GraphHopper Docker instance
     */
    private const val CLOUD_BASE_URL = "https://graphhopper.com/api/1/"
    private const val DEFAULT_SELF_HOSTED_BASE_URL = "http://localhost:8989/"

    /**
     * Current base URL (can be changed for self-hosted deployment).
     */
    private var baseUrl: String = CLOUD_BASE_URL

    /**
     * Optional API key (not required for cloud free tier, required for paid plans).
     */
    private var apiKey: String? = null

    /**
     * Configure the client for self-hosted GraphHopper instance.
     *
     * @param url Base URL of the self-hosted instance (e.g., "http://192.168.1.100:8989/")
     * @param key Optional API key if authentication is enabled
     */
    fun useSelfHosted(url: String = DEFAULT_SELF_HOSTED_BASE_URL, key: String? = null) {
        baseUrl = url
        apiKey = key
    }

    /**
     * Configure the client for cloud GraphHopper API.
     *
     * @param key Optional API key for paid plans (free tier doesn't need key)
     */
    fun useCloud(key: String? = null) {
        baseUrl = CLOUD_BASE_URL
        apiKey = key
    }

    /**
     * Get the current API key.
     */
    fun getApiKey(): String? = apiKey

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
     * Recreate if base URL changes.
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
     * Get the GraphHopper API service instance.
     */
    val service: GraphHopperApiService by lazy {
        getRetrofit().create(GraphHopperApiService::class.java)
    }

    /**
     * Force recreation of the service (e.g., after changing configuration).
     */
    fun recreateService() {
        retrofitInstance = null
    }
}
