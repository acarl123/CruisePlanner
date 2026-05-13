package com.example.cruiseplanner.data.navigation

/**
 * Represents a single turn-by-turn navigation instruction.
 *
 * @param text Human-readable instruction text (e.g., "Turn left onto Main Street")
 * @param distanceMeters Distance to this maneuver in meters
 * @param durationSeconds Estimated time to reach this maneuver in seconds
 * @param type The type of maneuver/instruction
 * @param maneuver Raw maneuver type from API (e.g., "turn-left", "merge")
 * @param roadName Name of the road/street for this instruction
 */
data class NavigationInstruction(
    val text: String,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val type: InstructionType,
    val maneuver: String? = null,
    val roadName: String? = null
)

/**
 * Enumeration of navigation instruction types.
 * Based on common turn-by-turn navigation maneuvers.
 */
enum class InstructionType {
    TURN_LEFT,
    TURN_RIGHT,
    TURN_SLIGHT_LEFT,
    TURN_SLIGHT_RIGHT,
    TURN_SHARP_LEFT,
    TURN_SHARP_RIGHT,
    CONTINUE_STRAIGHT,
    MERGE,
    ROUNDABOUT,
    ARRIVE,
    DEPART,
    UNKNOWN
}

/**
 * Utility object for parsing instruction types from text or maneuver strings.
 */
object InstructionParser {
    /**
     * Parses instruction type from maneuver string (OSRM format).
     *
     * Common OSRM maneuver types:
     * - turn: left, right, slight left, slight right, sharp left, sharp right, straight
     * - new name: continue
     * - depart / arrive
     * - merge: left, right, slight left, slight right
     * - roundabout: enter, exit
     *
     * @param maneuver The maneuver type from API (e.g., "turn", "merge")
     * @param modifier The modifier (e.g., "left", "right", "slight left")
     * @return The corresponding InstructionType
     */
    fun parseFromOsrm(maneuver: String?, modifier: String?): InstructionType {
        if (maneuver == null) return InstructionType.UNKNOWN

        return when (maneuver.lowercase()) {
            "turn" -> when (modifier?.lowercase()) {
                "left" -> InstructionType.TURN_LEFT
                "right" -> InstructionType.TURN_RIGHT
                "slight left", "slight-left" -> InstructionType.TURN_SLIGHT_LEFT
                "slight right", "slight-right" -> InstructionType.TURN_SLIGHT_RIGHT
                "sharp left", "sharp-left" -> InstructionType.TURN_SHARP_LEFT
                "sharp right", "sharp-right" -> InstructionType.TURN_SHARP_RIGHT
                "straight" -> InstructionType.CONTINUE_STRAIGHT
                else -> InstructionType.UNKNOWN
            }
            "new name", "continue" -> InstructionType.CONTINUE_STRAIGHT
            "merge" -> InstructionType.MERGE
            "roundabout", "rotary" -> InstructionType.ROUNDABOUT
            "arrive" -> InstructionType.ARRIVE
            "depart" -> InstructionType.DEPART
            else -> InstructionType.UNKNOWN
        }
    }

    /**
     * Parses instruction type from natural language text.
     * Used as fallback when structured maneuver data is unavailable.
     *
     * @param text The instruction text (e.g., "Turn left onto Main Street")
     * @return The best-guess InstructionType
     */
    fun parseFromText(text: String): InstructionType {
        val lowerText = text.lowercase()

        return when {
            lowerText.contains("turn left") -> InstructionType.TURN_LEFT
            lowerText.contains("turn right") -> InstructionType.TURN_RIGHT
            lowerText.contains("slight left") -> InstructionType.TURN_SLIGHT_LEFT
            lowerText.contains("slight right") -> InstructionType.TURN_SLIGHT_RIGHT
            lowerText.contains("sharp left") -> InstructionType.TURN_SHARP_LEFT
            lowerText.contains("sharp right") -> InstructionType.TURN_SHARP_RIGHT
            lowerText.contains("continue") || lowerText.contains("straight") -> InstructionType.CONTINUE_STRAIGHT
            lowerText.contains("merge") -> InstructionType.MERGE
            lowerText.contains("roundabout") || lowerText.contains("rotary") -> InstructionType.ROUNDABOUT
            lowerText.contains("arrive") || lowerText.contains("destination") -> InstructionType.ARRIVE
            lowerText.contains("depart") || lowerText.contains("start") -> InstructionType.DEPART
            else -> InstructionType.UNKNOWN
        }
    }
}
