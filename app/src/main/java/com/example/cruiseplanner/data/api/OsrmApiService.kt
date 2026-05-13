package com.example.cruiseplanner.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for OSRM routing API.
 * OSRM is completely free and doesn't require an API key.
 */
interface OsrmApiService {

    /**
     * Calculate a route between waypoints using OSRM.
     *
     * Format: /route/v1/{profile}/{coordinates}
     * Example: /route/v1/driving/-98.5795,39.8283;-87.6298,41.8781
     *
     * @param profile Routing profile: "driving", "car", "bike", "foot"
     * @param coordinates Semicolon-separated list of "lng,lat" pairs
     * @param overview Geometry overview: "full" for complete route, "simplified" for simplified
     * @param geometries Geometry format: "geojson" (default) or "polyline"
     * @return Route response with geometry
     */
    @GET("route/v1/{profile}/{coordinates}")
    suspend fun getRoute(
        @Path("profile") profile: String = "driving",
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson"
    ): OsrmResponse

    /**
     * Calculate a route with turn-by-turn instructions using OSRM.
     * Includes detailed step-by-step navigation instructions.
     *
     * @param profile Routing profile: "driving", "car", "bike", "foot"
     * @param coordinates Semicolon-separated list of "lng,lat" pairs
     * @param overview Geometry overview: "full" for complete route
     * @param geometries Geometry format: "geojson" (default) or "polyline"
     * @param steps Include turn-by-turn instructions (true for navigation)
     * @param annotations Include additional metadata (true for detailed navigation)
     * @return Route response with geometry and turn-by-turn instructions
     */
    @GET("route/v1/{profile}/{coordinates}")
    suspend fun getRouteWithSteps(
        @Path("profile") profile: String = "driving",
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("steps") steps: Boolean = true,
        @Query("annotations") annotations: Boolean = true
    ): OsrmResponse
}
