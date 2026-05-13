package com.example.cruiseplanner.data.navigation

import com.example.cruiseplanner.data.Route
import com.example.cruiseplanner.data.Waypoint

/**
 * Represents the current state of turn-by-turn navigation.
 * Uses a sealed class hierarchy to enforce mutually exclusive states.
 */
sealed class NavigationState {
    /**
     * Navigation is not active.
     */
    object Idle : NavigationState()

    /**
     * Navigation is actively running with real-time updates.
     *
     * @param route The route being navigated
     * @param currentWaypointIndex Index of the next waypoint to reach (0-based)
     * @param startedAtWaypointIndex Index where navigation started (for progress calculation)
     * @param distanceToNextWaypoint Distance in meters to the next waypoint
     * @param totalDistanceRemaining Total distance remaining in meters
     * @param estimatedTimeRemaining Estimated time remaining in seconds
     * @param currentInstruction Current turn-by-turn instruction (if available)
     * @param averageSpeed Average speed in m/s (used for ETA calculation)
     */
    data class Active(
        val route: Route,
        val currentWaypointIndex: Int,
        val startedAtWaypointIndex: Int,
        val distanceToNextWaypoint: Double,
        val totalDistanceRemaining: Double,
        val estimatedTimeRemaining: Long,
        val currentInstruction: NavigationInstruction? = null,
        val averageSpeed: Double = 0.0
    ) : NavigationState() {
        /**
         * The current target waypoint.
         */
        val currentWaypoint: Waypoint
            get() = route.waypoints[currentWaypointIndex]

        /**
         * Number of waypoints completed since navigation started.
         */
        val waypointsCompleted: Int
            get() = currentWaypointIndex - startedAtWaypointIndex

        /**
         * Total number of waypoints in the navigation session.
         */
        val waypointsTotal: Int
            get() = route.waypoints.size - startedAtWaypointIndex

        /**
         * Returns progress as a percentage (0-100).
         */
        fun getProgressPercentage(): Float {
            if (waypointsTotal == 0) return 0f
            return (waypointsCompleted.toFloat() / waypointsTotal) * 100f
        }
    }

    /**
     * Navigation has been completed.
     *
     * @param route The route that was navigated
     * @param startedAtWaypointIndex Index where navigation started
     * @param completedWaypoints Number of waypoints completed
     * @param totalDuration Total duration of navigation in milliseconds
     */
    data class Completed(
        val route: Route,
        val startedAtWaypointIndex: Int,
        val completedWaypoints: Int,
        val totalDuration: Long
    ) : NavigationState()
}
