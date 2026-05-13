package com.example.cruiseplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.cruiseplanner.data.RouteRepository
import com.example.cruiseplanner.ui.screens.MapScreen
import com.example.cruiseplanner.ui.screens.RouteListScreen
import com.example.cruiseplanner.ui.theme.CruisePlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CruisePlannerTheme {
                CruisePlannerApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun CruisePlannerApp() {
    val context = LocalContext.current
    val repository = remember { RouteRepository(context) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var routes by remember { mutableStateOf(repository.getAllRoutes()) }
    var selectedRoute by remember { mutableStateOf<com.example.cruiseplanner.data.Route?>(null) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.HOME -> {
                MapScreen(
                    existingRoute = selectedRoute,
                    onSaveRoute = { route ->
                        repository.saveRoute(route)
                        routes = repository.getAllRoutes()
                        selectedRoute = route
                    }
                )
            }
            AppDestinations.FAVORITES -> {
                RouteListScreen(
                    routes = routes,
                    onRouteClick = { route ->
                        selectedRoute = route
                        currentDestination = AppDestinations.HOME
                    },
                    onCreateRoute = {
                        selectedRoute = null
                        currentDestination = AppDestinations.HOME
                    }
                )
            }
            AppDestinations.PROFILE -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Profile - Coming Soon")
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Map", Icons.Default.Home),
    FAVORITES("Routes", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

