package com.example.cruiseplanner.ui.components

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.cruiseplanner.R
import com.example.cruiseplanner.data.LatLng
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Data class representing camera position on the map.
 */
data class CameraPosition(
    val center: LatLng,
    val zoom: Double = 15.0,
    val bearing: Float = 0f,  // Map rotation in degrees (0 = north up)
    val tilt: Float = 0f      // Future: 3D tilt (not yet implemented in osmdroid)
)

/**
 * Data class representing a map marker.
 */
data class MapMarker(
    val position: LatLng,
    val title: String = "",
    val snippet: String = "",
    val id: String = "",
    val number: Int? = null
)

/**
 * Data class representing a polyline on the map.
 */
data class MapPolyline(
    val points: List<LatLng>,
    val color: Color = Color.Blue,
    val width: Float = 10f
)

/**
 * Data class for map UI settings.
 */
data class MapUiSettings(
    val zoomControls: Boolean = true,
    val compass: Boolean = true,
    val myLocationButton: Boolean = true,
    val rotationGestures: Boolean = true,
    val scrollGestures: Boolean = true,
    val zoomGestures: Boolean = true
)

/**
 * Creates a custom numbered marker drawable.
 */
private fun createNumberedMarkerDrawable(context: Context, number: Int): Drawable {
    val size = 80
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw circle background
    val paint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#1976D2") // Blue color
        style = Paint.Style.FILL
    }
    val radius = size / 2.5f
    canvas.drawCircle(size / 2f, size / 2f, radius, paint)

    // Draw white border
    paint.apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawCircle(size / 2f, size / 2f, radius, paint)

    // Draw number
    val textPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = if (number < 10) 36f else 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textBounds = Rect()
    val numberText = number.toString()
    textPaint.getTextBounds(numberText, 0, numberText.length, textBounds)
    val textY = size / 2f - textBounds.exactCenterY()
    canvas.drawText(numberText, size / 2f, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Composable function that displays an OpenStreetMap using osmdroid.
 *
 * @param modifier Modifier for the map container
 * @param cameraPosition Camera position (center, zoom, bearing)
 * @param markers List of markers to display on the map
 * @param polylines List of polylines to draw on the map
 * @param uiSettings UI controls visibility settings
 * @param onMapClick Callback when the map is clicked
 * @param onMarkerClick Callback when a marker is clicked
 * @param onMarkerDrag Callback when a marker is dragged (provides marker ID and new position)
 * @param showMyLocation Whether to show the user's location
 * @param myLocationEnabled Whether location tracking is enabled
 * @param onMyLocationClick Callback when my location should be centered (provides current location)
 * @param markersAreDraggable Whether markers can be dragged
 * @param autoFollowLocation Whether to automatically follow user's location (navigation mode)
 * @param autoFollowBearing Optional bearing for map rotation during auto-follow
 * @param locationUpdateFrequencyMs Location update frequency in milliseconds (default 2000ms, use 500ms for navigation)
 */
@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition(LatLng(0.0, 0.0), 10.0),
    markers: List<MapMarker> = emptyList(),
    polylines: List<MapPolyline> = emptyList(),
    uiSettings: MapUiSettings = MapUiSettings(),
    onMapClick: ((LatLng) -> Unit)? = null,
    onMarkerClick: ((MapMarker) -> Unit)? = null,
    onMarkerDrag: ((String, LatLng) -> Unit)? = null,
    showMyLocation: Boolean = false,
    myLocationEnabled: Boolean = false,
    onMyLocationClick: ((LatLng?) -> Unit)? = null,
    markersAreDraggable: Boolean = false,
    autoFollowLocation: Boolean = false,
    autoFollowBearing: Float? = null,
    locationUpdateFrequencyMs: Long = 2000
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize osmdroid configuration
    DisposableEffect(context) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    // Create and remember the MapView
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)

            // Apply UI settings
            setBuiltInZoomControls(uiSettings.zoomControls)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false

            // Set min/max zoom levels
            minZoomLevel = 3.0
            maxZoomLevel = 20.0
        }
    }

    // Location overlay state
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Track previous camera position to avoid unnecessary updates
    var lastCameraPosition by remember { mutableStateOf(cameraPosition) }

    // Update location periodically if location is enabled
    LaunchedEffect(myLocationEnabled, locationOverlay, locationUpdateFrequencyMs) {
        if (myLocationEnabled && locationOverlay != null) {
            while (true) {
                delay(locationUpdateFrequencyMs)
                locationOverlay?.myLocation?.let { geoPoint ->
                    onMyLocationClick?.invoke(LatLng(geoPoint.latitude, geoPoint.longitude))
                }
            }
        }
    }

    // Enable/disable auto-follow based on navigation state
    LaunchedEffect(autoFollowLocation, locationOverlay) {
        if (autoFollowLocation) {
            locationOverlay?.enableFollowLocation()
        } else {
            locationOverlay?.disableFollowLocation()
        }
    }

    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    locationOverlay?.enableMyLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    locationOverlay?.disableMyLocation()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { factoryContext ->
            mapView.apply {
                // Set up map click listener
                setOnClickListener {
                    // Map tap handling is done via overlay
                    true
                }

                // Add tap listener overlay
                overlays.add(object : org.osmdroid.views.overlay.Overlay() {
                    override fun onSingleTapConfirmed(
                        e: android.view.MotionEvent,
                        mapView: MapView
                    ): Boolean {
                        val projection = mapView.projection
                        val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                        onMapClick?.invoke(LatLng(geoPoint.latitude, geoPoint.longitude))
                        return true
                    }
                })

                // Set up location overlay if needed
                if (showMyLocation || myLocationEnabled) {
                    val myLocationOverlay = MyLocationNewOverlay(
                        GpsMyLocationProvider(factoryContext),
                        this
                    ).apply {
                        enableMyLocation()
                        // Don't auto-follow location, let user control it
                        disableFollowLocation()
                    }
                    locationOverlay = myLocationOverlay
                    overlays.add(myLocationOverlay)
                }
            }
            mapView
        },
        update = { view ->
            // Update camera position only if it changed
            if (cameraPosition != lastCameraPosition) {
                view.controller.setZoom(cameraPosition.zoom)
                view.controller.setCenter(cameraPosition.center.toGeoPoint())
                lastCameraPosition = cameraPosition
            }

            // Apply map rotation based on bearing
            // Note: osmdroid uses negative bearing (clockwise rotation)
            val targetBearing = autoFollowBearing ?: cameraPosition.bearing
            view.mapOrientation = -targetBearing

            // Clear existing marker and polyline overlays (keep system overlays)
            val systemOverlays = view.overlays.filter { overlay ->
                overlay is MyLocationNewOverlay ||
                overlay !is Marker && overlay !is Polyline
            }
            view.overlays.clear()
            view.overlays.addAll(systemOverlays)

            // Add polylines (draw first so they appear under markers)
            polylines.forEach { polyline ->
                val osmPolyline = Polyline(view).apply {
                    outlinePaint.color = polyline.color.toArgb()
                    outlinePaint.strokeWidth = polyline.width
                    setPoints(polyline.points.map { it.toGeoPoint() })
                }
                view.overlays.add(osmPolyline)
            }

            // Add markers
            markers.forEach { mapMarker ->
                val marker = Marker(view).apply {
                    position = mapMarker.position.toGeoPoint()
                    title = mapMarker.title
                    snippet = mapMarker.snippet
                    isDraggable = markersAreDraggable

                    // Set click listener
                    setOnMarkerClickListener { clickedMarker, _ ->
                        onMarkerClick?.invoke(mapMarker)
                        true
                    }

                    // Set drag listener
                    if (markersAreDraggable) {
                        setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                            override fun onMarkerDrag(marker: Marker) {
                                // Called during drag - can be used for visual feedback
                            }

                            override fun onMarkerDragEnd(marker: Marker) {
                                // Called when drag ends - update waypoint position
                                val newPosition = LatLng(marker.position.latitude, marker.position.longitude)
                                onMarkerDrag?.invoke(mapMarker.id, newPosition)
                            }

                            override fun onMarkerDragStart(marker: Marker) {
                                // Called when drag starts
                            }
                        })
                    }

                    // Use numbered marker icon if number is provided, otherwise use default
                    icon = if (mapMarker.number != null) {
                        createNumberedMarkerDrawable(view.context, mapMarker.number)
                    } else {
                        try {
                            ContextCompat.getDrawable(
                                view.context,
                                org.osmdroid.library.R.drawable.marker_default
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                view.overlays.add(marker)
            }

            // Refresh the map
            view.invalidate()
        }
    )
}

/**
 * Remember a CameraPosition state.
 */
@Composable
fun rememberCameraPositionState(
    initialPosition: LatLng = LatLng(0.0, 0.0),
    initialZoom: Double = 10.0
): MutableState<CameraPosition> {
    return remember {
        mutableStateOf(CameraPosition(initialPosition, initialZoom))
    }
}
