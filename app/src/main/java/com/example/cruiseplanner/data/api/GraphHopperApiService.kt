package com.example.cruiseplanner.data.api

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit service interface for GraphHopper routing API.
 */
interface GraphHopperApiService {

    /**
     * Calculate a route between waypoints.
     *
     * @param apiKey Optional API key (not required for cloud free tier, required for self-hosted)
     * @param request Route request with waypoints and options
     * @return Route response with paths and geometry
     */
    @POST("route")
    suspend fun getRoute(
        @Query("key") apiKey: String? = null,
        @Body request: GraphHopperRouteRequest
    ): GraphHopperResponse
}
