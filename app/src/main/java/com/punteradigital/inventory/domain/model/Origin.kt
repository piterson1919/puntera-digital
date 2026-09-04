package com.punteradigital.inventory.domain.model

/**
 * Origin/owner of the merchandise.
 * Controls the visual theme and UUID prefix.
 */
enum class Origin(val prefix: String, val displayName: String) {
    FOOT_SAFE("FS", "Foot Safe"),
    SAFETY("SF", "Safety");

    companion object {
        /**
         * Detects origin from a UUID prefix.
         * FS-xxxx → FOOT_SAFE
         * SF-xxxx → SAFETY
         */
        fun fromUuid(uuid: String): Origin? = when {
            uuid.startsWith("FS-") -> FOOT_SAFE
            uuid.startsWith("SF-") -> SAFETY
            else -> null
        }

        /**
         * Safely parses origin from a string.
         * Handles name, displayName, and prefix (case-insensitive).
         * Falls back to FOOT_SAFE to avoid runtime crashes.
         */
        fun fromString(value: String): Origin {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                it.displayName.equals(value, ignoreCase = true) ||
                it.prefix.equals(value, ignoreCase = true)
            } ?: FOOT_SAFE
        }
    }
}
