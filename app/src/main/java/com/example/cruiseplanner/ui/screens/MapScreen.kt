package com.example.cruiseplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import android.view.WindowManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.cruiseplanner.data.LatLng
import com.example.cruiseplanner.data.Route
import com.example.cruiseplanner.data.RoutingRepository
import com.example.cruiseplanner.data.Waypoint
import com.example.cruiseplanner.data.api.NominatimApiClient
import com.example.cruiseplanner.ui.components.CameraPosition
import com.example.cruiseplanner.ui.components.MapMarker
import com.example.cruiseplanner.ui.components.MapPolyline
import com.example.cruiseplanner.ui.components.MapUiSettings
import com.example.cruiseplanner.ui.components.OsmMapView
import com.example.cruiseplanner.ui.components.WaypointEditCard
import com.example.cruiseplanner.ui.components.WaypointPosition
import com.example.cruiseplanner.ui.components.WaypointReorderSheet
import com.example.cruiseplanner.ui.components.rememberCameraPositionState
import com.example.cruiseplanner.ui.components.NavigationPanel
import com.example.cruiseplanner.ui.components.StartNavigationDialog
import com.example.cruiseplanner.data.navigation.NavigationState
import com.example.cruiseplanner.data.navigation.NavigationManager
import com.example.cruiseplanner.data.navigation.LocationUpdate
import com.example.cruiseplanner.data.navigation.BearingCalculator
import com.example.cruiseplanner.data.navigation.NavigationInstruction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.*

/**
 * Calculate distance between two coordinates using Haversine formula.
 * Returns distance in kilometers.
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0 // Earth's radius in kilometers
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

/**
 * Trims a polyline to only show the remaining path from the current location.
 * Finds the closest point on the polyline and returns points from there to the end.
 *
 * @param polylinePoints The complete route polyline
 * @param currentLocation The user's current location
 * @return List of points from current position to end, or original list if currentLocation is null
 */
fun trimPolylineToRemainingPath(
    polylinePoints: List<LatLng>,
    currentLocation: LatLng?
): List<LatLng> {
    if (currentLocation == null || polylinePoints.isEmpty()) {
        return polylinePoints
    }

    // Find the closest point on the polyline to the current location
    var minDistance = Double.MAX_VALUE
    var closestIndex = 0

    polylinePoints.forEachIndexed { index, point ->
        val distance = calculateDistance(
            currentLocation.lat, currentLocation.lng,
            point.lat, point.lng
        ) * 1000.0 // Convert to meters

        if (distance < minDistance) {
            minDistance = distance
            closestIndex = index
        }
    }

    // Return the polyline from the closest point onward, starting with current location
    return if (closestIndex < polylinePoints.size) {
        listOf(currentLocation) + polylinePoints.subList(closestIndex, polylinePoints.size)
    } else {
        polylinePoints
    }
}

/**
 * Calculate camera position to fit all points in view.
 * Returns CameraPosition with center and appropriate zoom level.
 */
fun calculateCameraPositionForPoints(points: List<LatLng>): CameraPosition? {
    if (points.isEmpty()) return null

    if (points.size == 1) {
        return CameraPosition(center = points.first(), zoom = 15.0)
    }

    // Calculate bounds
    val minLat = points.minOf { it.lat }
    val maxLat = points.maxOf { it.lat }
    val minLng = points.minOf { it.lng }
    val maxLng = points.maxOf { it.lng }

    // Calculate center
    val centerLat = (minLat + maxLat) / 2
    val centerLng = (minLng + maxLng) / 2

    // Calculate span
    val latSpan = maxLat - minLat
    val lngSpan = maxLng - minLng
    val maxSpan = max(latSpan, lngSpan)

    // Calculate appropriate zoom level
    // Zoom levels: each level doubles the area shown
    // Rough approximation: zoom = log2(360 / span) - 1
    val zoom = if (maxSpan > 0) {
        val calculatedZoom = log2(360.0 / maxSpan) - 1.5 // -1.5 gives padding around route
        calculatedZoom.coerceIn(4.0, 18.0) // Limit zoom between 4 (country) and 18 (street)
    } else {
        15.0
    }

    return CameraPosition(
        center = LatLng(centerLat, centerLng),
        zoom = zoom
    )
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    existingRoute: Route? = null,
    onSaveRoute: (Route) -> Unit = {}
) {
    // Default location: Center of United States
    val defaultLocation = LatLng(39.8283, -98.5795)

    // If there's an existing route, center on its first waypoint
    val initialLocation = existingRoute?.waypoints?.firstOrNull()?.latLng ?: defaultLocation
    val initialZoom = if (existingRoute != null) 12.0 else 4.0

    val cameraPositionState = rememberCameraPositionState(
        initialPosition = initialLocation,
        initialZoom = initialZoom
    )

    // Location permission handling
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    // State for waypoints - load from existing route if available
    val waypoints = remember(existingRoute) {
        mutableStateListOf<Waypoint>().apply {
            existingRoute?.waypoints?.let { addAll(it) }
        }
    }
    var isAddingMode by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var routeName by remember(existingRoute) { mutableStateOf(existingRoute?.name ?: "") }
    var routeDescription by remember(existingRoute) { mutableStateOf(existingRoute?.description ?: "") }
    val routeId = remember(existingRoute) { existingRoute?.id }

    // Bottom sheet state for waypoint reordering
    var showWaypointSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Routing state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val routingRepository = remember { RoutingRepository(context) }

    var polylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoadingDirections by remember { mutableStateOf(false) }
    var directionsError by remember { mutableStateOf<String?>(null) }
    var routeDistance by remember { mutableStateOf(0L) }
    var routeDuration by remember { mutableStateOf(0L) }
    var fetchJob by remember { mutableStateOf<Job?>(null) }

    // Current user location state
    var currentUserLocation by remember { mutableStateOf<LatLng?>(null) }

    // Double-tap detection for removing waypoints
    var lastClickedMarkerId by remember { mutableStateOf<String?>(null) }
    var lastClickTime by remember { mutableStateOf(0L) }

    // Navigation state
    var navigationState by remember { mutableStateOf<NavigationState>(NavigationState.Idle) }
    val navigationManager = remember { NavigationManager() }
    var showStartNavigationDialog by remember { mutableStateOf(false) }
    var navigationInstructions by remember { mutableStateOf<List<NavigationInstruction>>(emptyList()) }
    var lastLocationUpdate by remember { mutableStateOf<LocationUpdate?>(null) }
    var previousLocation by remember { mutableStateOf<LatLng?>(null) }

    // Track initial load to prevent auto-zoom after user interaction
    var hasPerformedInitialZoom by remember { mutableStateOf(false) }

    // Computed navigation properties
    val isNavigating = navigationState is NavigationState.Active
    val hasLocationPermission = locationPermissionState.status.isGranted

    // Calculate display polyline: full route when not navigating, trimmed when navigating
    val displayPolylinePoints = remember(polylinePoints, isNavigating, currentUserLocation) {
        if (isNavigating && currentUserLocation != null) {
            trimPolylineToRemainingPath(polylinePoints, currentUserLocation)
        } else {
            polylinePoints
        }
    }

    // Search state
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<com.example.cruiseplanner.data.api.NominatimSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchForStart by remember { mutableStateOf(true) } // true for start, false for end
    val nominatimService = remember { NominatimApiClient.service }

    // Search function
    val performSearch: () -> Unit = {
        if (searchQuery.isNotBlank()) {
            isSearching = true
            scope.launch {
                try {
                    // Get reference point (user location or map center)
                    val referencePoint = currentUserLocation ?: cameraPositionState.value.center

                    // Create viewbox around current view (helps prioritize nearby results)
                    val viewboxRadius = 2.0 // degrees (~220km)
                    val viewbox = "${referencePoint.lng - viewboxRadius},${referencePoint.lat - viewboxRadius}," +
                                  "${referencePoint.lng + viewboxRadius},${referencePoint.lat + viewboxRadius}"

                    // Search with country code and viewbox
                    val results = nominatimService.search(
                        query = searchQuery,
                        countrycodes = "us",
                        viewbox = viewbox,
                        bounded = 0
                    )

                    // Sort results by distance from reference point
                    searchResults = results.sortedBy { result ->
                        calculateDistance(
                            referencePoint.lat,
                            referencePoint.lng,
                            result.lat.toDouble(),
                            result.lon.toDouble()
                        )
                    }
                } catch (e: Exception) {
                    searchResults = emptyList()
                }
                isSearching = false
            }
        }
    }

    val uiSettings = remember(hasLocationPermission) {
        MapUiSettings(
            zoomControls = true,
            myLocationButton = hasLocationPermission,
            compass = true,
            rotationGestures = true,
            scrollGestures = true,
            zoomGestures = true
        )
    }

    // Load existing route polyline and zoom to fit it (only once on initial load)
    LaunchedEffect(existingRoute) {
        existingRoute?.let { route ->
            if (route.polylinePoints.isNotEmpty()) {
                polylinePoints = route.polylinePoints
                routeDistance = route.distanceMeters
                routeDuration = route.durationSeconds

                // Zoom to fit the entire route (only once)
                if (!hasPerformedInitialZoom) {
                    calculateCameraPositionForPoints(route.polylinePoints)?.let { position ->
                        cameraPositionState.value = position
                        hasPerformedInitialZoom = true
                    }
                }
            } else if (route.waypoints.isNotEmpty()) {
                // If no polyline, zoom to fit waypoints (only once)
                if (!hasPerformedInitialZoom) {
                    calculateCameraPositionForPoints(route.waypoints.map { it.latLng })?.let { position ->
                        cameraPositionState.value = position
                        hasPerformedInitialZoom = true
                    }
                }
            }
        }
    }

    // Zoom to current location when no route is loaded (only once on initial load)
    LaunchedEffect(currentUserLocation, existingRoute) {
        val location = currentUserLocation
        if (existingRoute == null && location != null && !hasPerformedInitialZoom) {
            cameraPositionState.value = CameraPosition(
                center = location,
                zoom = 13.0
            )
            hasPerformedInitialZoom = true
        }
    }

    // Debounced auto-fetch on waypoint changes
    LaunchedEffect(waypoints.toList()) {
        fetchJob?.cancel()

        if (waypoints.size >= 2) {
            fetchJob = scope.launch {
                delay(500) // Debounce 500ms
                isLoadingDirections = true
                directionsError = null

                routingRepository.fetchDirections(waypoints.toList())
                    .onSuccess { result ->
                        polylinePoints = result.polylinePoints
                        routeDistance = result.distanceMeters
                        routeDuration = result.durationSeconds
                    }
                    .onFailure { error ->
                        directionsError = error.message
                        // Fallback to straight lines
                        polylinePoints = waypoints.map { it.latLng }
                    }

                isLoadingDirections = false
            }
        } else {
            polylinePoints = emptyList()
            routeDistance = 0L
            routeDuration = 0L
        }
    }

    // Navigation: Track bearing from location updates
    LaunchedEffect(currentUserLocation, previousLocation) {
        if (currentUserLocation != null && previousLocation != null) {
            val bearing = BearingCalculator.calculateBearing(previousLocation!!, currentUserLocation!!)
            lastLocationUpdate = LocationUpdate(
                position = currentUserLocation!!,
                bearing = bearing,
                speed = null,
                accuracy = null
            )
        }
        previousLocation = currentUserLocation
    }

    // Navigation: Update navigation state when location changes
    LaunchedEffect(navigationState, currentUserLocation) {
        if (navigationState is NavigationState.Active && currentUserLocation != null) {
            val activeState = navigationState as NavigationState.Active

            // Check if user has arrived at the current waypoint
            if (navigationManager.hasArrivedAtWaypoint(currentUserLocation!!, activeState.currentWaypoint)) {
                // Auto-advance to next waypoint
                navigationState = navigationManager.advanceToNextWaypoint(activeState, currentUserLocation!!)
            } else {
                // Update metrics (distance, ETA, speed)
                navigationState = navigationManager.updateNavigationState(
                    activeState,
                    currentUserLocation!!,
                    lastLocationUpdate
                )
            }
        }
    }

    // Navigation: Calculate camera position for auto-follow
    val targetCameraPosition = remember(navigationState, currentUserLocation, lastLocationUpdate) {
        when (navigationState) {
            is NavigationState.Active -> {
                currentUserLocation?.let { location ->
                    CameraPosition(
                        center = location,
                        zoom = 19.0, // Close zoom for detailed turn-by-turn navigation
                        bearing = lastLocationUpdate?.bearing ?: 0f
                    )
                } ?: cameraPositionState.value
            }
            else -> cameraPositionState.value
        }
    }

    // Navigation: Apply camera position during navigation (auto-follow)
    LaunchedEffect(targetCameraPosition, isNavigating) {
        if (isNavigating && navigationState is NavigationState.Active) {
            cameraPositionState.value = targetCameraPosition
        }
    }

    // Keep screen on during navigation
    val view = LocalView.current
    DisposableEffect(isNavigating) {
        if (isNavigating) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            cameraPosition = cameraPositionState.value,
            markers = waypoints.mapIndexed { index, waypoint ->
                MapMarker(
                    position = waypoint.latLng,
                    title = "${index + 1}. ${waypoint.name ?: "Waypoint"}",
                    snippet = "Tap to view • Double-tap to delete",
                    id = waypoint.id,
                    number = index + 1
                )
            },
            polylines = if (displayPolylinePoints.size > 1) {
                listOf(
                    MapPolyline(
                        points = displayPolylinePoints,
                        color = if (isLoadingDirections) Color.Gray else Color.Blue,
                        width = 10f
                    )
                )
            } else {
                emptyList()
            },
            uiSettings = uiSettings,
            onMapClick = { latLng ->
                if (isAddingMode && !isNavigating) {
                    // Add a new waypoint (only when not navigating)
                    waypoints.add(
                        Waypoint(
                            id = UUID.randomUUID().toString(),
                            latLng = latLng,
                            name = "Waypoint ${waypoints.size + 1}",
                            order = waypoints.size
                        )
                    )
                }
            },
            showMyLocation = hasLocationPermission,
            myLocationEnabled = hasLocationPermission,
            onMyLocationClick = { location ->
                // Store the current location when it becomes available
                location?.let { currentUserLocation = it }
            },
            onMarkerClick = { marker ->
                val currentTime = System.currentTimeMillis()
                val timeDiff = currentTime - lastClickTime

                // Double-tap detection: if same marker clicked within 500ms
                if (marker.id == lastClickedMarkerId && timeDiff < 500) {
                    // Double-tap detected - remove the waypoint
                    waypoints.removeAll { it.id == marker.id }
                    // Reset double-tap detection
                    lastClickedMarkerId = null
                    lastClickTime = 0L
                } else {
                    // Single tap - just record it for double-tap detection
                    lastClickedMarkerId = marker.id
                    lastClickTime = currentTime
                }
            },
            onMarkerDrag = { markerId, newPosition ->
                // Update the waypoint position when dragged
                val index = waypoints.indexOfFirst { it.id == markerId }
                if (index >= 0) {
                    val oldWaypoint = waypoints[index]
                    waypoints[index] = oldWaypoint.copy(latLng = newPosition)
                }
            },
            markersAreDraggable = !isAddingMode && !isNavigating,  // Disable dragging during navigation
            autoFollowLocation = isNavigating,
            autoFollowBearing = if (isNavigating) lastLocationUpdate?.bearing else null,
            locationUpdateFrequencyMs = if (isNavigating) 500 else 2000
        )

        // Top control panel - Show navigation panel when navigating
        if (isNavigating && navigationState is NavigationState.Active) {
            NavigationPanel(
                navigationState = navigationState as NavigationState.Active,
                onExitNavigation = {
                    navigationState = NavigationState.Idle
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        } else {
            // Regular control panel
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                // Location permission request button
                if (!hasLocationPermission) {
                    Button(
                        onClick = { locationPermissionState.launchPermissionRequest() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable Location to use location button")
                    }
                }

                // Loading and error indicators
                if (isLoadingDirections) {
                    Text(
                        text = "Loading directions...",
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.Gray
                    )
                }

                directionsError?.let { error ->
                    Text(
                        text = "Error: $error",
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.Red
                    )
                }

                // Edit mode UI
                if (isAddingMode) {
                // Cancel button at top
                OutlinedButton(
                    onClick = { isAddingMode = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null
                    )
                    Text(
                        text = "Cancel",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Waypoint cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Start waypoint card
                    WaypointEditCard(
                        waypoint = waypoints.firstOrNull(),
                        position = WaypointPosition.START,
                        onSearchClick = {
                            searchForStart = true
                            showSearchDialog = true
                        }
                    )

                    // End waypoint card
                    WaypointEditCard(
                        waypoint = if (waypoints.size >= 2) waypoints.last() else null,
                        position = WaypointPosition.END,
                        onSearchClick = {
                            searchForStart = false
                            showSearchDialog = true
                        },
                        distanceText = if (waypoints.size >= 2 && routeDistance > 0) {
                            "%.1f mi total".format(routeDistance / 1609.34)
                        } else null
                    )
                }

                Text(
                    text = "Tap on the map to add waypoints",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Action buttons row
                if (waypoints.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reorder button (only show when 2+ waypoints)
                        if (waypoints.size >= 2) {
                            OutlinedButton(
                                onClick = { showWaypointSheet = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reorder")
                            }
                        }

                        // Undo last waypoint
                        OutlinedButton(
                            onClick = {
                                if (waypoints.isNotEmpty()) {
                                    waypoints.removeAt(waypoints.size - 1)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Undo")
                        }

                        // Clear all waypoints
                        OutlinedButton(
                            onClick = {
                                waypoints.clear()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                }

                // Save button (prominent when ready to save)
                if (waypoints.size >= 2) {
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Route")
                    }
                }
            } else {
                // View mode: Show Edit Route and Start Navigation buttons
                Button(
                    onClick = { isAddingMode = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                    Text(
                        text = "Edit Route",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Start Navigation button (only show if route has waypoints and location permission)
                if (waypoints.size >= 2 && hasLocationPermission) {
                    Button(
                        onClick = { showStartNavigationDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Navigation")
                    }
                }
            }

                    if (waypoints.isNotEmpty()) {
                        val distanceText = if (routeDistance > 0) {
                            " • %.1f mi • %d min".format(routeDistance / 1609.34, routeDuration / 60)
                        } else ""
                        Text(
                            text = "${waypoints.size} waypoints$distanceText",
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Location button - centers camera on user's current location (hide during navigation)
        if (hasLocationPermission && currentUserLocation != null && !isNavigating) {
            FloatingActionButton(
                onClick = {
                    currentUserLocation?.let { location ->
                        cameraPositionState.value = CameraPosition(
                            center = location,
                            zoom = 15.0
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "My Location"
                )
            }
        }

        // Save dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Route") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = routeName,
                            onValueChange = { routeName = it },
                            label = { Text("Route Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = routeDescription,
                            onValueChange = { routeDescription = it },
                            label = { Text("Description (Optional)") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (routeName.isNotBlank()) {
                                val route = if (routeId != null) {
                                    // Update existing route
                                    existingRoute!!.copy(
                                        name = routeName,
                                        description = routeDescription,
                                        waypoints = waypoints.toList(),
                                        polylinePoints = polylinePoints,
                                        distanceMeters = routeDistance,
                                        durationSeconds = routeDuration
                                    )
                                } else {
                                    // Create new route
                                    Route(
                                        name = routeName,
                                        description = routeDescription,
                                        waypoints = waypoints.toList(),
                                        polylinePoints = polylinePoints,
                                        distanceMeters = routeDistance,
                                        durationSeconds = routeDuration
                                    )
                                }
                                onSaveRoute(route)
                                // Exit edit mode and zoom to first waypoint
                                showSaveDialog = false
                                isAddingMode = false
                                // Zoom to first waypoint to show the saved route
                                waypoints.firstOrNull()?.let { firstWaypoint ->
                                    cameraPositionState.value = CameraPosition(
                                        center = firstWaypoint.latLng,
                                        zoom = 12.0
                                    )
                                }
                            }
                        },
                        enabled = routeName.isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Search dialog
        if (showSearchDialog) {
            AlertDialog(
                onDismissRequest = {
                    showSearchDialog = false
                    searchQuery = ""
                    searchResults = emptyList()
                },
                title = { Text(if (searchForStart) "Search Start Location" else "Search End Location") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Enter address or location") },
                            placeholder = { Text("e.g., 123 Main St, City, State") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { performSearch() }
                            ),
                            trailingIcon = {
                                IconButton(onClick = performSearch) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }
                        )

                        if (isSearching) {
                            Text("Searching...", color = Color.Gray)
                        }

                        if (searchResults.isEmpty() && !isSearching && searchQuery.isNotBlank()) {
                            Text(
                                "No results found. Try:\n• Full address: 123 Main St, City, ST ZIP\n• Just city and state: City, ST\n• Landmark name",
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Show search results
                        searchResults.forEach { result ->
                            val location = LatLng(
                                lat = result.lat.toDouble(),
                                lng = result.lon.toDouble()
                            )

                            // Calculate distance from reference point
                            val referencePoint = currentUserLocation ?: cameraPositionState.value.center
                            val distanceKm = calculateDistance(
                                referencePoint.lat,
                                referencePoint.lng,
                                location.lat,
                                location.lng
                            )
                            val distanceMiles = distanceKm * 0.621371 // Convert to miles

                            TextButton(
                                onClick = {
                                    if (searchForStart) {
                                        // Add as first waypoint or replace first
                                        if (waypoints.isEmpty()) {
                                            waypoints.add(
                                                Waypoint(
                                                    id = UUID.randomUUID().toString(),
                                                    latLng = location,
                                                    name = "Start",
                                                    order = 0
                                                )
                                            )
                                        } else {
                                            waypoints[0] = waypoints[0].copy(latLng = location)
                                        }
                                    } else {
                                        // Add as last waypoint or replace last
                                        if (waypoints.size < 2) {
                                            waypoints.add(
                                                Waypoint(
                                                    id = UUID.randomUUID().toString(),
                                                    latLng = location,
                                                    name = "End",
                                                    order = waypoints.size
                                                )
                                            )
                                        } else {
                                            val lastIndex = waypoints.size - 1
                                            waypoints[lastIndex] = waypoints[lastIndex].copy(latLng = location)
                                        }
                                    }

                                    // Center camera on the selected location
                                    cameraPositionState.value = CameraPosition(
                                        center = location,
                                        zoom = 13.0
                                    )

                                    showSearchDialog = false
                                    searchQuery = ""
                                    searchResults = emptyList()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = result.displayName,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "%.1f mi away".format(distanceMiles),
                                        color = Color.Gray,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showSearchDialog = false
                        searchQuery = ""
                        searchResults = emptyList()
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Start Navigation Dialog
        if (showStartNavigationDialog) {
            StartNavigationDialog(
                route = Route(
                    name = routeName.ifBlank { "Navigation Route" },
                    description = routeDescription,
                    waypoints = waypoints.toList(),
                    polylinePoints = polylinePoints,
                    distanceMeters = routeDistance,
                    durationSeconds = routeDuration
                ),
                onDismiss = { showStartNavigationDialog = false },
                onStartNavigation = { startingIndex ->
                    scope.launch {
                        // Fetch directions with turn-by-turn instructions
                        routingRepository.fetchDirectionsWithInstructions(waypoints.toList())
                            .onSuccess { result ->
                                navigationInstructions = result.instructions
                                navigationState = NavigationState.Active(
                                    route = Route(
                                        name = routeName.ifBlank { "Navigation Route" },
                                        description = routeDescription,
                                        waypoints = waypoints.toList(),
                                        polylinePoints = result.polylinePoints,
                                        distanceMeters = result.distanceMeters,
                                        durationSeconds = result.durationSeconds
                                    ),
                                    currentWaypointIndex = startingIndex,
                                    startedAtWaypointIndex = startingIndex,
                                    distanceToNextWaypoint = if (currentUserLocation != null && startingIndex < waypoints.size) {
                                        BearingCalculator.calculateDistance(currentUserLocation!!, waypoints[startingIndex].latLng)
                                    } else 0.0,
                                    totalDistanceRemaining = result.distanceMeters.toDouble(),
                                    estimatedTimeRemaining = result.durationSeconds,
                                    currentInstruction = result.instructions.firstOrNull()
                                )
                            }
                            .onFailure { error ->
                                // Show error, but don't start navigation
                                directionsError = "Failed to load navigation data: ${error.message}"
                            }
                    }
                    showStartNavigationDialog = false
                }
            )
        }

        // Waypoint reorder bottom sheet
        if (showWaypointSheet) {
            ModalBottomSheet(
                onDismissRequest = { showWaypointSheet = false },
                sheetState = sheetState,
                modifier = Modifier.navigationBarsPadding()
            ) {
                WaypointReorderSheet(
                    waypoints = waypoints,
                    onReorder = { fromIndex, toIndex ->
                        val waypoint = waypoints.removeAt(fromIndex)
                        waypoints.add(toIndex, waypoint)
                        waypoints.forEachIndexed { index, wp ->
                            waypoints[index] = wp.copy(order = index)
                        }
                    },
                    onDelete = { waypoint ->
                        waypoints.removeAll { it.id == waypoint.id }
                    },
                    onWaypointClick = { waypoint ->
                        cameraPositionState.value = CameraPosition(
                            center = waypoint.latLng,
                            zoom = 15.0
                        )
                    }
                )
            }
        }
    }
}
