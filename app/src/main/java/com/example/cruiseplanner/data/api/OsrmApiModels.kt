package com.example.cruiseplanner.data.api

import com.google.gson.annotations.SerializedName

/**
 * Response model from OSRM routing API.
 */
data class OsrmResponse(
    @SerializedName("code")
    val code: String,  // "Ok" on success

    @SerializedName("routes")
    val routes: List<OsrmRoute>,

    @SerializedName("waypoints")
    val waypoints: List<OsrmWaypoint>? = null
)

/**
 * A single route from OSRM.
 */
data class OsrmRoute(
    @SerializedName("distance")
    val distance: Double,  // Distance in meters

    @SerializedName("duration")
    val duration: Double,  // Duration in seconds

    @SerializedName("geometry")
    val geometry: OsrmGeometry,

    @SerializedName("legs")
    val legs: List<OsrmLeg>? = null  // Turn-by-turn instructions (requires steps=true)
)

/**
 * Route geometry (polyline or GeoJSON).
 */
data class OsrmGeometry(
    @SerializedName("coordinates")
    val coordinates: List<List<Double>>  // [[lng, lat], [lng, lat], ...]
)

/**
 * Waypoint information from OSRM.
 */
data class OsrmWaypoint(
    @SerializedName("location")
    val location: List<Double>,  // [lng, lat]

    @SerializedName("name")
    val name: String? = null
)

/**
 * A leg of a route between two waypoints.
 * Contains detailed turn-by-turn steps.
 */
data class OsrmLeg(
    @SerializedName("steps")
    val steps: List<OsrmStep>? = null,

    @SerializedName("distance")
    val distance: Double,  // Distance in meters

    @SerializedName("duration")
    val duration: Double,  // Duration in seconds

    @SerializedName("summary")
    val summary: String? = null
)

/**
 * A single step/instruction within a route leg.
 */
data class OsrmStep(
    @SerializedName("distance")
    val distance: Double,  // Distance for this step in meters

    @SerializedName("duration")
    val duration: Double,  // Duration for this step in seconds

    @SerializedName("name")
    val name: String? = null,  // Road/street name

    @SerializedName("maneuver")
    val maneuver: OsrmManeuver? = null,

    @SerializedName("mode")
    val mode: String? = null,  // "driving", "walking", etc.

    @SerializedName("geometry")
    val geometry: OsrmGeometry? = null
)

/**
 * Maneuver information for a step (turn, merge, etc.).
 */
data class OsrmManeuver(
    @SerializedName("type")
    val type: String,  // "turn", "merge", "roundabout", "arrive", "depart", etc.

    @SerializedName("modifier")
    val modifier: String? = null,  // "left", "right", "slight left", "sharp right", etc.

    @SerializedName("instruction")
    val instruction: String? = null,  // Human-readable instruction

    @SerializedName("bearing_after")
    val bearingAfter: Int? = null,  // Bearing after maneuver in degrees

    @SerializedName("location")
    val location: List<Double>? = null  // [lng, lat] of maneuver point
)
