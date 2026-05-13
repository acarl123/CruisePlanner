package com.example.cruiseplanner.utils

import com.example.cruiseplanner.data.LatLng

/**
 * Utility class for decoding GeoJSON coordinate arrays.
 * GeoJSON uses [longitude, latitude] order (not [latitude, longitude]).
 */
object GeoJsonDecoder {

    /**
     * Decodes a GeoJSON LineString coordinate array into a list of LatLng points.
     *
     * @param coordinates Array of [longitude, latitude] pairs from GeoJSON
     * @return List of LatLng objects
     * @throws IllegalArgumentException if coordinate format is invalid
     */
    fun decodeLineString(coordinates: List<List<Double>>): List<LatLng> {
        return coordinates.mapNotNull { coord ->
            if (coord.size >= 2) {
                val lng = coord[0]
                val lat = coord[1]
                LatLng(lat = lat, lng = lng)
            } else {
                null
            }
        }
    }

    /**
     * Decodes a single GeoJSON coordinate pair into a LatLng.
     *
     * @param coordinate [longitude, latitude] pair from GeoJSON
     * @return LatLng object
     * @throws IllegalArgumentException if coordinate format is invalid
     */
    fun decodePoint(coordinate: List<Double>): LatLng {
        require(coordinate.size >= 2) {
            "Invalid coordinate format. Expected [longitude, latitude] but got: $coordinate"
        }
        val lng = coordinate[0]
        val lat = coordinate[1]
        return LatLng(lat = lat, lng = lng)
    }

    /**
     * Encodes a LatLng into GeoJSON coordinate format [longitude, latitude].
     *
     * @param latLng The LatLng to encode
     * @return List containing [longitude, latitude]
     */
    fun encodePoint(latLng: LatLng): List<Double> {
        return listOf(latLng.lng, latLng.lat)
    }

    /**
     * Encodes a list of LatLng into GeoJSON LineString coordinate format.
     *
     * @param points List of LatLng objects
     * @return List of [longitude, latitude] pairs
     */
    fun encodeLineString(points: List<LatLng>): List<List<Double>> {
        return points.map { encodePoint(it) }
    }
}
