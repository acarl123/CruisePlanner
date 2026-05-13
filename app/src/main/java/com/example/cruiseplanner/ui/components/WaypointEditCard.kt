package com.example.cruiseplanner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cruiseplanner.data.Waypoint

enum class WaypointPosition {
    START, END, INTERMEDIATE
}

sealed class WaypointAction {
    object Navigate : WaypointAction()
    object Edit : WaypointAction()
    object Remove : WaypointAction()
    object Pin : WaypointAction()
}

@Composable
fun WaypointEditCard(
    waypoint: Waypoint?,
    position: WaypointPosition,
    onSearchClick: () -> Unit,
    onActionClick: (WaypointAction) -> Unit = {},
    showActions: Boolean = false,
    distanceText: String? = null,
    modifier: Modifier = Modifier
) {
    if (waypoint == null) {
        EmptyWaypointCard(
            position = position,
            onSearchClick = onSearchClick,
            modifier = modifier
        )
    } else {
        FilledWaypointCard(
            waypoint = waypoint,
            position = position,
            onSearchClick = onSearchClick,
            onActionClick = onActionClick,
            showActions = showActions,
            distanceText = distanceText,
            modifier = modifier
        )
    }
}

@Composable
private fun EmptyWaypointCard(
    position: WaypointPosition,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val positionLabel = when (position) {
        WaypointPosition.START -> "Start"
        WaypointPosition.END -> "End"
        WaypointPosition.INTERMEDIATE -> "Waypoint"
    }

    val positionBadge = when (position) {
        WaypointPosition.START -> "S"
        WaypointPosition.END -> "E"
        WaypointPosition.INTERMEDIATE -> "I"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSearchClick)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = positionBadge,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Empty state content
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$positionLabel location",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FilledWaypointCard(
    waypoint: Waypoint,
    position: WaypointPosition,
    onSearchClick: () -> Unit,
    onActionClick: (WaypointAction) -> Unit,
    showActions: Boolean,
    distanceText: String?,
    modifier: Modifier = Modifier
) {
    val positionLabel = when (position) {
        WaypointPosition.START -> "Start"
        WaypointPosition.END -> "End"
        WaypointPosition.INTERMEDIATE -> "Waypoint"
    }

    val positionBadge = when (position) {
        WaypointPosition.START -> "S"
        WaypointPosition.END -> "E"
        WaypointPosition.INTERMEDIATE -> "I"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSearchClick)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = positionBadge,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Waypoint info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = positionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = waypoint.name ?: "Unnamed location",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                distanceText?.let { distance ->
                    Text(
                        text = distance,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
