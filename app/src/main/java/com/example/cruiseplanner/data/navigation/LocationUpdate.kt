package com.example.cruiseplanner.data.navigation

import com.example.cruiseplanner.data.LatLng
import kotlin.math.*

/**
 * Represents a location update with additional navigation-relevant data.
 *
 * @param position The geographic position
 * @param bearing Direction of travel in degrees from north (0-360), null if unavailable
 * @param speed Current speed in meters per second, null if unavailable
 * @param accuracy Horizontal accuracy in meters, null if unavailable
 * @param timestamp Unix timestamp in milliseconds when this update was received
 */
data class LocationUpdate(
    val position: LatLng,
    val bearing: Float? = null,
    val speed: Float? = null,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Utility object for bearing and distance calculations using the Haversine formula.
 */
object BearingCalculator {
    private const val EARTH_RADIUS_METERS = 6371000.0 // Earth's mean radius in meters

    /**
     * Calculates the bearing (direction) from one point to another.
     * Returns the initial bearing (forward azimuth) in degrees from north (0-360).
     *
     * Uses the Haversine formula for accuracy over short distances.
     *
     * @param from Starting point
     * @param to Ending point
     * @return Bearing in degrees (0-360), where 0 is north, 90 is east, 180 is south, 270 is west
     */
    fun calculateBearing(from: LatLng, to: LatLng): Float {
        val startLat = Math.toRadians(from.lat)
        val startLng = Math.toRadians(from.lng)
        val endLat = Math.toRadians(to.lat)
        val endLng = Math.toRadians(to.lng)

        val dLng = endLng - startLng

        val y = sin(dLng) * cos(endLat)
        val x = cos(startLat) * sin(endLat) - sin(startLat) * cos(endLat) * cos(dLng)

        val bearing = Math.toDegrees(atan2(y, x))

        // Normalize to 0-360 range
        return ((bearing + 360) % 360).toFloat()
    }

    /**
     * Calculates the great-circle distance between two points using the Haversine formula.
     *
     * @param from Starting point
     * @param to Ending point
     * @return Distance in meters
     */
    fun calculateDistance(from: LatLng, to: LatLng): Double {
        val lat1 = Math.toRadians(from.lat)
        val lat2 = Math.toRadians(to.lat)
        val dLat = Math.toRadians(to.lat - from.lat)
        val dLng = Math.toRadians(to.lng - from.lng)

        val a = sin(dLat / 2).pow(2) +
                cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Checks if two points are within a specified distance threshold.
     *
     * @param from First point
     * @param to Second point
     * @param thresholdMeters Distance threshold in meters
     * @return True if the distance is less than or equal to the threshold
     */
    fun isWithinDistance(from: LatLng, to: LatLng, thresholdMeters: Double): Boolean {
        return calculateDistance(from, to) <= thresholdMeters
    }

    /**
     * Calculates the time to reach a destination based on current speed.
     *
     * @param distanceMeters Distance to destination in meters
     * @param speedMetersPerSecond Current speed in m/s
     * @return Estimated time in seconds, or null if speed is invalid
     */
    fun calculateETA(distanceMeters: Double, speedMetersPerSecond: Double): Long? {
        if (speedMetersPerSecond <= 0) return null
        return (distanceMeters / speedMetersPerSecond).toLong()
    }
}
