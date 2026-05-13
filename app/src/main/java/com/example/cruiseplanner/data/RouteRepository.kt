package com.example.cruiseplanner.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.Date

class RouteRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LatLng::class.java, LatLngTypeAdapter())
        .registerTypeAdapter(Date::class.java, DateTypeAdapter())
        .create()

    fun saveRoute(route: Route) {
        val routes = getAllRoutes().toMutableList()
        val existingIndex = routes.indexOfFirst { it.id == route.id }

        if (existingIndex >= 0) {
            routes[existingIndex] = route
        } else {
            routes.add(route)
        }

        saveAllRoutes(routes)
    }

    fun getAllRoutes(): List<Route> {
        val json = prefs.getString(KEY_ROUTES, null) ?: return emptyList()
        val type = object : TypeToken<List<Route>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getRouteById(id: String): Route? {
        return getAllRoutes().find { it.id == id }
    }

    fun deleteRoute(routeId: String) {
        val routes = getAllRoutes().toMutableList()
        routes.removeIf { it.id == routeId }
        saveAllRoutes(routes)
    }

    private fun saveAllRoutes(routes: List<Route>) {
        val json = gson.toJson(routes)
        prefs.edit().putString(KEY_ROUTES, json).apply()
    }

    companion object {
        private const val PREFS_NAME = "cruise_planner_prefs"
        private const val KEY_ROUTES = "routes"
    }
}

// Type adapter for LatLng serialization
// Maintains {"lat": X, "lng": Y} format for backward compatibility
class LatLngTypeAdapter : JsonSerializer<LatLng>, JsonDeserializer<LatLng> {
    override fun serialize(
        src: LatLng,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val obj = JsonObject()
        obj.addProperty("lat", src.lat)
        obj.addProperty("lng", src.lng)
        return obj
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LatLng {
        val obj = json.asJsonObject
        return LatLng(
            lat = obj.get("lat").asDouble,
            lng = obj.get("lng").asDouble
        )
    }
}

// Type adapter for Date serialization
class DateTypeAdapter : JsonSerializer<Date>, JsonDeserializer<Date> {
    override fun serialize(
        src: Date,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return context.serialize(src.time)
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Date {
        return Date(json.asLong)
    }
}
