package com.example.cruiseplanner.data.api

import com.google.gson.annotations.SerializedName

/**
 * Request model for GraphHopper routing API.
 *
 * @param points List of coordinate pairs in [longitude, latitude] format (GeoJSON standard)
 * @param profile Routing profile: "car", "bike", "foot", etc.
 * @param locale Language for instructions (e.g., "en", "de")
 * @param calcPoints Whether to include detailed route geometry
 * @param pointsEncoded Whether to encode points (we want false for easy parsing)
 * @param instructions Whether to include turn-by-turn instructions
 * @param elevation Whether to include elevation data
 * @param customModel Optional custom routing model for scenic routes (Phase 8)
 */
data class GraphHopperRouteRequest(
    @SerializedName("points")
    val points: List<List<Double>>,

    @SerializedName("profile")
    val profile: String = "car",

    @SerializedName("locale")
    val locale: String = "en",

    @SerializedName("calc_points")
    val calcPoints: Boolean = true,

    @SerializedName("points_encoded")
    val pointsEncoded: Boolean = false,

    @SerializedName("instructions")
    val instructions: Boolean = false,

    @SerializedName("elevation")
    val elevation: Boolean = false,

    @SerializedName("custom_model")
    val customModel: CustomModel? = null
)

/**
 * Custom routing model for scenic routes (used in Phase 8).
 * This allows fine-tuning the routing algorithm to prefer certain road types.
 */
data class CustomModel(
    @SerializedName("distance_influence")
    val distanceInfluence: Double? = null,

    @SerializedName("priority")
    val priority: List<PriorityRule>? = null,

    @SerializedName("speed")
    val speed: List<SpeedRule>? = null
)

/**
 * Priority rule for custom routing model.
 * Example: { "if": "road_class == MOTORWAY", "multiply_by": 0.5 } to avoid highways
 */
data class PriorityRule(
    @SerializedName("if")
    val condition: String,

    @SerializedName("multiply_by")
    val multiplyBy: Double
)

/**
 * Speed rule for custom routing model.
 */
data class SpeedRule(
    @SerializedName("if")
    val condition: String,

    @SerializedName("multiply_by")
    val multiplyBy: Double
)

/**
 * Response model from GraphHopper routing API.
 */
data class GraphHopperResponse(
    @SerializedName("paths")
    val paths: List<Path>,

    @SerializedName("info")
    val info: Info? = null
)

/**
 * A single route path from the response.
 */
data class Path(
    @SerializedName("distance")
    val distance: Double,  // Distance in meters

    @SerializedName("time")
    val time: Long,  // Duration in milliseconds

    @SerializedName("points")
    val points: Points,

    @SerializedName("snapped_waypoints")
    val snappedWaypoints: Points? = null,

    @SerializedName("instructions")
    val instructions: List<Instruction>? = null
)

/**
 * Route geometry as GeoJSON.
 * When points_encoded=false, coordinates is a simple array of [longitude, latitude] pairs.
 */
data class Points(
    @SerializedName("type")
    val type: String,  // "LineString"

    @SerializedName("coordinates")
    val coordinates: List<List<Double>>  // [[lng, lat], [lng, lat], ...]
)

/**
 * Turn-by-turn instruction (optional, not used in basic routing).
 */
data class Instruction(
    @SerializedName("text")
    val text: String,

    @SerializedName("distance")
    val distance: Double,

    @SerializedName("time")
    val time: Long
)

/**
 * Metadata about the routing request.
 */
data class Info(
    @SerializedName("copyrights")
    val copyrights: List<String>? = null,

    @SerializedName("took")
    val took: Long? = null  // Processing time in ms
)
