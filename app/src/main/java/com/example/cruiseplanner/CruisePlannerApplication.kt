package com.example.cruiseplanner

import android.app.Application
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Application class for CruisePlanner.
 * Configures OpenStreetMap (osmdroid) settings on app startup.
 */
class CruisePlannerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure osmdroid
        configureOsmdroid()
    }

    private fun configureOsmdroid() {
        // Set user agent to identify our app to tile servers
        Configuration.getInstance().userAgentValue = packageName

        // Configure tile cache
        val osmConfig = Configuration.getInstance()

        // Set cache directory
        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath

        // Set tile cache directory
        val tileCache = File(osmConfig.osmdroidBasePath, "tile")
        osmConfig.osmdroidTileCache = tileCache

        // Set cache size to 100MB (helps with performance)
        osmConfig.tileFileSystemCacheMaxBytes = 100L * 1024L * 1024L // 100 MB

        // Trim cache to 75MB when it exceeds max (prevents frequent cache clearing)
        osmConfig.tileFileSystemCacheTrimBytes = 75L * 1024L * 1024L // 75 MB

        // Enable debug logging (disable in production if needed)
        // Log.d("CruisePlanner", "osmdroid cache path: ${tileCache.absolutePath}")
    }
}
