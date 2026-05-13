package com.example.cruiseplanner.data

import android.content.Context
import com.example.cruiseplanner.data.api.OsrmApiClient
import com.example.cruiseplanner.data.navigation.NavigationInstruction
import com.example.cruiseplanner.data.navigation.InstructionParser
import com.example.cruiseplanner.utils.GeoJsonDecoder

data class DirectionsResult(
    val polylinePoints: List<LatLng>,
    val distanceMeters: Long,
    val durationSeconds: Long,
    val instructions: List<NavigationInstruction> = emptyList()
)

class RoutingRepository(private val context: Context) {

    private val apiService = OsrmApiClient.service

    suspend fun fetchDirections(waypoints: List<Waypoint>): Result<DirectionsResult> {
        return try {
            // Validate minimum waypoints
            if (waypoints.size < 2) {
                return Result.failure(IllegalArgumentException("At least 2 waypoints required"))
            }

            // Validate coordinates are in valid range
            waypoints.forEach { waypoint ->
                if (waypoint.latLng.lat < -90 || waypoint.latLng.lat > 90) {
                    return Result.failure(IllegalArgumentException("Invalid latitude: ${waypoint.latLng.lat}"))
                }
                if (waypoint.latLng.lng < -180 || waypoint.latLng.lng > 180) {
                    return Result.failure(IllegalArgumentException("Invalid longitude: ${waypoint.latLng.lng}"))
                }
            }

            // Convert waypoints to OSRM coordinate format: "lng,lat;lng,lat;lng,lat"
            val coordinates = waypoints.joinToString(";") { waypoint ->
                "${waypoint.latLng.lng},${waypoint.latLng.lat}"
            }

            // Call OSRM API (completely free, no API key needed)
            val response = apiService.getRoute(
                profile = "driving",
                coordinates = coordinates,
                overview = "full",
                geometries = "geojson"
            )

            // Check response status
            if (response.code != "Ok") {
                return Result.failure(Exception("Routing failed: ${response.code}"))
            }

            // Extract the best route
            val route = response.routes.firstOrNull()
                ?: return Result.failure(Exception("No routes found"))

            // Parse GeoJSON coordinates
            val polylinePoints = GeoJsonDecoder.decodeLineString(route.geometry.coordinates)

            // Get distance (in meters) and duration (in seconds)
            val distanceMeters = route.distance.toLong()
            val durationSeconds = route.duration.toLong()

            Result.success(
                DirectionsResult(
                    polylinePoints = polylinePoints,
                    distanceMeters = distanceMeters,
                    durationSeconds = durationSeconds
                )
            )
        } catch (e: retrofit2.HttpException) {
            // Handle HTTP errors
            when (e.code()) {
                400 -> Result.failure(Exception("Invalid request. Check waypoint coordinates."))
                429 -> Result.failure(Exception("API rate limit exceeded. Please try again later."))
                500, 503 -> Result.failure(Exception("OSRM service unavailable. Please try again later."))
                else -> Result.failure(Exception("API error (${e.code()}): ${e.message()}"))
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("No internet connection. Please check your network."))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Request timed out. Please try again."))
        } catch (e: Exception) {
            Result.failure(Exception("Routing error: ${e.message}", e))
        }
    }

    /**
     * Fetches directions with turn-by-turn navigation instructions.
     * This method includes detailed step-by-step instructions for navigation.
     *
     * @param waypoints List of waypoints to route through (minimum 2)
     * @return Result containing route geometry, distance, duration, and turn-by-turn instructions
     */
    suspend fun fetchDirectionsWithInstructions(waypoints: List<Waypoint>): Result<DirectionsResult> {
        return try {
            // Validate minimum waypoints
            if (waypoints.size < 2) {
                return Result.failure(IllegalArgumentException("At least 2 waypoints required"))
            }

            // Validate coordinates are in valid range
            waypoints.forEach { waypoint ->
                if (waypoint.latLng.lat < -90 || waypoint.latLng.lat > 90) {
                    return Result.failure(IllegalArgumentException("Invalid latitude: ${waypoint.latLng.lat}"))
                }
                if (waypoint.latLng.lng < -180 || waypoint.latLng.lng > 180) {
                    return Result.failure(IllegalArgumentException("Invalid longitude: ${waypoint.latLng.lng}"))
                }
            }

            // Convert waypoints to OSRM coordinate format: "lng,lat;lng,lat;lng,lat"
            val coordinates = waypoints.joinToString(";") { waypoint ->
                "${waypoint.latLng.lng},${waypoint.latLng.lat}"
            }

            // Call OSRM API with steps enabled for turn-by-turn instructions
            val response = apiService.getRouteWithSteps(
                profile = "driving",
                coordinates = coordinates,
                overview = "full",
                geometries = "geojson",
                steps = true,
                annotations = true
            )

            // Check response status
            if (response.code != "Ok") {
                return Result.failure(Exception("Routing failed: ${response.code}"))
            }

            // Extract the best route
            val route = response.routes.firstOrNull()
                ?: return Result.failure(Exception("No routes found"))

            // Parse GeoJSON coordinates
            val polylinePoints = GeoJsonDecoder.decodeLineString(route.geometry.coordinates)

            // Get distance (in meters) and duration (in seconds)
            val distanceMeters = route.distance.toLong()
            val durationSeconds = route.duration.toLong()

            // Parse turn-by-turn instructions from legs/steps
            val instructions = parseInstructions(route.legs)

            Result.success(
                DirectionsResult(
                    polylinePoints = polylinePoints,
                    distanceMeters = distanceMeters,
                    durationSeconds = durationSeconds,
                    instructions = instructions
                )
            )
        } catch (e: retrofit2.HttpException) {
            // Handle HTTP errors
            when (e.code()) {
                400 -> Result.failure(Exception("Invalid request. Check waypoint coordinates."))
                429 -> Result.failure(Exception("API rate limit exceeded. Please try again later."))
                500, 503 -> Result.failure(Exception("OSRM service unavailable. Please try again later."))
                else -> Result.failure(Exception("API error (${e.code()}): ${e.message()}"))
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("No internet connection. Please check your network."))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Request timed out. Please try again."))
        } catch (e: Exception) {
            Result.failure(Exception("Routing error: ${e.message}", e))
        }
    }

    /**
     * Parses OSRM legs and steps into NavigationInstruction objects.
     *
     * @param legs List of route legs (each leg is between two waypoints)
     * @return List of navigation instructions
     */
    private fun parseInstructions(legs: List<com.example.cruiseplanner.data.api.OsrmLeg>?): List<NavigationInstruction> {
        if (legs == null) return emptyList()

        val instructions = mutableListOf<NavigationInstruction>()

        legs.forEach { leg ->
            leg.steps?.forEach { step ->
                val maneuver = step.maneuver
                if (maneuver != null) {
                    // Parse instruction type from maneuver
                    val instructionType = InstructionParser.parseFromOsrm(
                        maneuver.type,
                        maneuver.modifier
                    )

                    // Generate instruction text
                    val instructionText = maneuver.instruction
                        ?: generateInstructionText(maneuver.type, maneuver.modifier, step.name)

                    instructions.add(
                        NavigationInstruction(
                            text = instructionText,
                            distanceMeters = step.distance,
                            durationSeconds = step.duration.toLong(),
                            type = instructionType,
                            maneuver = maneuver.type,
                            roadName = step.name
                        )
                    )
                }
            }
        }

        return instructions
    }

    /**
     * Generates human-readable instruction text from maneuver data.
     * Used as fallback when API doesn't provide instruction text.
     *
     * @param type Maneuver type (e.g., "turn", "merge")
     * @param modifier Maneuver modifier (e.g., "left", "right")
     * @param roadName Name of the road/street
     * @return Human-readable instruction text
     */
    private fun generateInstructionText(type: String?, modifier: String?, roadName: String?): String {
        val action = when (type?.lowercase()) {
            "turn" -> when (modifier?.lowercase()) {
                "left" -> "Turn left"
                "right" -> "Turn right"
                "slight left", "slight-left" -> "Turn slight left"
                "slight right", "slight-right" -> "Turn slight right"
                "sharp left", "sharp-left" -> "Turn sharp left"
                "sharp right", "sharp-right" -> "Turn sharp right"
                "straight" -> "Continue straight"
                else -> "Turn"
            }
            "merge" -> "Merge"
            "roundabout", "rotary" -> "Enter roundabout"
            "arrive" -> "Arrive at destination"
            "depart" -> "Depart"
            "continue", "new name" -> "Continue"
            else -> "Continue"
        }

        return if (!roadName.isNullOrBlank()) {
            "$action onto $roadName"
        } else {
            action
        }
    }
}
