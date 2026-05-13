package com.example.cruiseplanner.data

import java.util.Date
import java.util.UUID

data class Route(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val waypoints: List<Waypoint> = emptyList(),
    val polylinePoints: List<LatLng> = emptyList(),
    val distanceMeters: Long = 0L,
    val durationSeconds: Long = 0L,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val createdBy: String? = null,
    val isShared: Boolean = false,
    val clubId: String? = null
) {
    fun getDistanceInMiles(): Double {
        return distanceMeters / 1609.34
    }

    fun getDurationInMinutes(): Long {
        return durationSeconds / 60
    }
}
