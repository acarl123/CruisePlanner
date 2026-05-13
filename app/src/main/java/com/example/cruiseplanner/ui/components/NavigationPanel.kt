package com.example.cruiseplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cruiseplanner.data.navigation.InstructionType
import com.example.cruiseplanner.data.navigation.NavigationState
import kotlin.math.roundToInt

/**
 * Navigation panel that displays turn-by-turn instructions in Google Maps style.
 * Shown during active navigation mode.
 */
@Composable
fun NavigationPanel(
    navigationState: NavigationState.Active,
    onExitNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Exit button only (top-right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onExitNavigation) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Navigation",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Main turn instruction (Google Maps style - BIG and prominent)
            navigationState.currentInstruction?.let { instruction ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large turn icon
                    Icon(
                        imageVector = getInstructionIcon(instruction.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Large distance text
                    if (instruction.distanceMeters > 0) {
                        Text(
                            text = formatDistance(instruction.distanceMeters),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Instruction text
                    Text(
                        text = instruction.text,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            } ?: run {
                // When no instruction available, show "Head to destination"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = formatDistance(navigationState.distanceToNextWaypoint),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Head toward ${navigationState.currentWaypoint.name ?: "destination"}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom info bar: ETA and total distance (smaller, secondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Total distance remaining
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatDistance(navigationState.totalDistanceRemaining),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Divider
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )

                // ETA
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatDuration(navigationState.estimatedTimeRemaining),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "ETA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Maps instruction type to Material icon.
 * Uses available Material Icons that approximate navigation directions.
 */
fun getInstructionIcon(type: InstructionType): ImageVector {
    return when (type) {
        InstructionType.TURN_LEFT -> Icons.Default.ArrowBack
        InstructionType.TURN_RIGHT -> Icons.Default.ArrowForward
        InstructionType.TURN_SLIGHT_LEFT -> Icons.Default.ArrowBack
        InstructionType.TURN_SLIGHT_RIGHT -> Icons.Default.ArrowForward
        InstructionType.TURN_SHARP_LEFT -> Icons.Default.ArrowBack
        InstructionType.TURN_SHARP_RIGHT -> Icons.Default.ArrowForward
        InstructionType.CONTINUE_STRAIGHT -> Icons.Default.ArrowForward
        InstructionType.MERGE -> Icons.Default.ArrowForward
        InstructionType.ROUNDABOUT -> Icons.Default.Refresh
        InstructionType.ARRIVE -> Icons.Default.Place
        InstructionType.DEPART -> Icons.Default.Star
        InstructionType.UNKNOWN -> Icons.Default.Info
    }
}

/**
 * Formats distance in meters to human-readable string.
 * Uses miles (imperial units).
 */
fun formatDistance(meters: Double): String {
    val miles = meters / 1609.34

    return when {
        miles < 0.1 -> "${(meters * 3.28084).roundToInt()} ft"
        miles < 1.0 -> "${(miles * 10).roundToInt() / 10.0} mi"
        else -> "${miles.roundToInt()} mi"
    }
}

/**
 * Formats duration in seconds to human-readable string.
 */
fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes} min"
        else -> "< 1 min"
    }
}
