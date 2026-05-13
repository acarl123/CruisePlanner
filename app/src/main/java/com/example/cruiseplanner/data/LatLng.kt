package com.example.cruiseplanner.data

import org.osmdroid.util.GeoPoint

/**
 * Custom LatLng data class to decouple from mapping library dependencies.
 * JSON format: {"lat": X, "lng": Y} - compatible with Google Maps format for backward compatibility.
 */
data class LatLng(
    val lat: Double,
    val lng: Double
) {
    /**
     * Converts this LatLng to an osmdroid GeoPoint.
     */
    fun toGeoPoint(): GeoPoint {
        return GeoPoint(lat, lng)
    }

    companion object {
        /**
         * Creates a LatLng from an osmdroid GeoPoint.
         */
        fun fromGeoPoint(geoPoint: GeoPoint): LatLng {
            return LatLng(geoPoint.latitude, geoPoint.longitude)
        }
    }

    override fun toString(): String {
        return "LatLng(lat=$lat, lng=$lng)"
    }
}
