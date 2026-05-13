package com.example.cruiseplanner.data

data class Waypoint(
    val id: String,
    val latLng: LatLng,
    val name: String? = null,
    val description: String? = null,
    val order: Int = 0
)
