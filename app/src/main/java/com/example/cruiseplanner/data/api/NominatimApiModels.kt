package com.example.cruiseplanner.data.api

import com.google.gson.annotations.SerializedName

/**
 * Response model from Nominatim geocoding API.
 */
data class NominatimSearchResult(
    @SerializedName("place_id")
    val placeId: Long,

    @SerializedName("lat")
    val lat: String,

    @SerializedName("lon")
    val lon: String,

    @SerializedName("display_name")
    val displayName: String,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("importance")
    val importance: Double? = null
)
