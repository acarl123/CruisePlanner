package com.example.cruiseplanner.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for Nominatim geocoding API.
 * Nominatim is OpenStreetMap's free geocoding service.
 */
interface NominatimApiService {

    /**
     * Search for locations by query string.
     *
     * @param query Search query (e.g., "New York", "1600 Amphitheatre Parkway, Mountain View, CA")
     * @param format Response format (json, xml, jsonv2, etc.)
     * @param limit Maximum number of results
     * @param addressdetails Include detailed address breakdown (1 = yes, 0 = no)
     * @param countrycodes Limit results to specific countries (e.g., "us" for USA)
     * @param viewbox Preferred area to find results (minlon,minlat,maxlon,maxlat)
     * @param bounded Restrict results to viewbox area (1 = yes, 0 = no)
     * @return List of search results
     */
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 10,
        @Query("addressdetails") addressdetails: Int = 1,
        @Query("countrycodes") countrycodes: String? = null,
        @Query("viewbox") viewbox: String? = null,
        @Query("bounded") bounded: Int? = null
    ): List<NominatimSearchResult>
}
