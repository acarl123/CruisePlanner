package com.example.cruiseplanner.data.navigation

import com.example.cruiseplanner.data.LatLng
import com.example.cruiseplanner.data.Waypoint
import kotlin.math.max

/**
 * Manages the core business logic for turn-by-turn navigation.
 * Handles waypoint proximity detection, automatic advancement, and state updates.
 */
class NavigationManager(
    private val proximityThresholdMeters: Double = DEFAULT_PROXIMITY_THRESHOLD
) {
    companion object {
        const val DEFAULT_PROXIMITY_THRESHOLD = 30.0 // 30 meters (similar to Google Maps)
        private const val MIN_SPEED_THRESHOLD = 0.5 // Minimum speed in m/s to consider for ETA
        private const val SPEED_SMOOTHING_FACTOR = 0.3 // For exponential moving average
    }

    /**
     * Checks if the user has arrived at a waypoint based on proximity threshold.
     *
     * @param currentLocation The user's current location
     * @param waypoint The target waypoint to check
     * @return True if within proximity threshold
     */
    fun hasArrivedAtWaypoint(currentLocation: LatLng, waypoint: Waypoint): Boolean {
        return BearingCalculator.isWithinDistance(
            currentLocation,
            waypoint.latLng,
            proximityThresholdMeters
        )
    }

    /**
     * Advances to the next waypoint in the route.
     * Returns updated navigation state (either new Active state or Completed state).
     *
     * @param currentState The current active navigation state
     * @param currentLocation The user's current location
     * @return Updated navigation state (Active with next waypoint, or Completed)
     */
    fun advanceToNextWaypoint(
        currentState: NavigationState.Active,
        currentLocation: LatLng
    ): NavigationState {
        val nextIndex = currentState.currentWaypointIndex + 1

        // Check if this was the last waypoint
        if (nextIndex >= currentState.route.waypoints.size) {
            return NavigationState.Completed(
                route = currentState.route,
                startedAtWaypointIndex = currentState.startedAtWaypointIndex,
                completedWaypoints = currentState.waypointsCompleted + 1,
                totalDuration = System.currentTimeMillis() // You may want to track start time
            )
        }

        // Advance to next waypoint
        val nextWaypoint = currentState.route.waypoints[nextIndex]
        val distanceToNext = BearingCalculator.calculateDistance(currentLocation, nextWaypoint.latLng)

        // Find the next instruction for the new waypoint segment
        val nextInstruction = findNextInstruction(currentState, nextIndex)

        // Recalculate total remaining distance
        val remainingDistance = calculateRemainingDistance(
            currentState.route,
            nextIndex,
            currentLocation
        )

        // Calculate ETA based on average speed
        val eta = if (currentState.averageSpeed > MIN_SPEED_THRESHOLD) {
            (remainingDistance / currentState.averageSpeed).toLong()
        } else {
            currentState.estimatedTimeRemaining
        }

        return currentState.copy(
            currentWaypointIndex = nextIndex,
            distanceToNextWaypoint = distanceToNext,
            totalDistanceRemaining = remainingDistance,
            estimatedTimeRemaining = eta,
            currentInstruction = nextInstruction
        )
    }

    /**
     * Updates navigation state with current location and metrics.
     * Recalculates distance, ETA, and speed.
     *
     * @param currentState The current active navigation state
     * @param currentLocation The user's current location
     * @param locationUpdate Optional location update with bearing/speed info
     * @return Updated navigation state with refreshed metrics
     */
    fun updateNavigationState(
        currentState: NavigationState.Active,
        currentLocation: LatLng,
        locationUpdate: LocationUpdate?
    ): NavigationState.Active {
        // Calculate distance to next waypoint
        val distanceToNext = BearingCalculator.calculateDistance(
            currentLocation,
            currentState.currentWaypoint.latLng
        )

        // Calculate total remaining distance
        val remainingDistance = calculateRemainingDistance(
            currentState.route,
            currentState.currentWaypointIndex,
            currentLocation
        )

        // Update average speed using exponential moving average
        val currentSpeed = locationUpdate?.speed?.toDouble() ?: 0.0
        val updatedAverageSpeed = if (currentState.averageSpeed > 0) {
            (SPEED_SMOOTHING_FACTOR * currentSpeed) +
                    ((1 - SPEED_SMOOTHING_FACTOR) * currentState.averageSpeed)
        } else {
            currentSpeed
        }

        // Calculate ETA
        val eta = if (updatedAverageSpeed > MIN_SPEED_THRESHOLD) {
            (remainingDistance / updatedAverageSpeed).toLong()
        } else {
            currentState.estimatedTimeRemaining
        }

        // Update instruction based on distance (if instructions are distance-based)
        val updatedInstruction = updateCurrentInstruction(
            currentState.currentInstruction,
            distanceToNext
        )

        return currentState.copy(
            distanceToNextWaypoint = distanceToNext,
            totalDistanceRemaining = remainingDistance,
            estimatedTimeRemaining = eta,
            averageSpeed = updatedAverageSpeed,
            currentInstruction = updatedInstruction
        )
    }

    /**
     * Calculates the total remaining distance from current location to route end.
     *
     * @param route The navigation route
     * @param currentWaypointIndex Index of the next waypoint to reach
     * @param currentLocation User's current location
     * @return Total remaining distance in meters
     */
    private fun calculateRemainingDistance(
        route: com.example.cruiseplanner.data.Route,
        currentWaypointIndex: Int,
        currentLocation: LatLng
    ): Double {
        if (currentWaypointIndex >= route.waypoints.size) return 0.0

        var totalDistance = 0.0

        // Distance to next waypoint
        totalDistance += BearingCalculator.calculateDistance(
            currentLocation,
            route.waypoints[currentWaypointIndex].latLng
        )

        // Distance between remaining waypoints
        for (i in currentWaypointIndex until route.waypoints.size - 1) {
            totalDistance += BearingCalculator.calculateDistance(
                route.waypoints[i].latLng,
                route.waypoints[i + 1].latLng
            )
        }

        return totalDistance
    }

    /**
     * Finds the next navigation instruction for a given waypoint segment.
     * This is a placeholder - actual implementation would match instructions to segments.
     *
     * @param currentState Current navigation state
     * @param waypointIndex Index of the waypoint
     * @return The navigation instruction, or null if not available
     */
    private fun findNextInstruction(
        currentState: NavigationState.Active,
        waypointIndex: Int
    ): NavigationInstruction? {
        // TODO: Match instructions to waypoint segments based on route geometry
        // For now, return null (instructions will be added in Phase 2)
        return null
    }

    /**
     * Updates the current instruction based on distance to next maneuver.
     * This allows for progressive instruction updates (e.g., "In 500m turn left").
     *
     * @param currentInstruction Current instruction
     * @param distanceToWaypoint Distance to next waypoint in meters
     * @return Updated instruction, or null
     */
    private fun updateCurrentInstruction(
        currentInstruction: NavigationInstruction?,
        distanceToWaypoint: Double
    ): NavigationInstruction? {
        // For now, just return the current instruction unchanged
        // Future enhancement: Update instruction text based on distance
        // (e.g., "In 500m turn left" -> "In 100m turn left" -> "Turn left now")
        return currentInstruction
    }
}
