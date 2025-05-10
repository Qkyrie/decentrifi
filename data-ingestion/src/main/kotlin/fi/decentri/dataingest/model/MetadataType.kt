package fi.decentri.dataingest.model

import java.time.Duration
import java.time.Instant

/**
 * Enum representing the types of metadata tracked during ingestion
 * This helps prevent errors from typos in string literals
 */
enum class MetadataType(val key: String) {
    LAST_PROCESSED_BLOCK_RAW_INVOCATIONS("last_processed_block_raw_invocations"),
    LAST_PROCESSED_BLOCK_EVENTS("last_processed_block_events"),
    EVENTS_LAST_RUN_TIMESTAMP("events_last_run_timestamp"),
    RAW_INVOCATIONS_LAST_RUN_TIMESTAMP("raw_invocations_last_run_timestamp");

    companion object {
        /**
         * Get enum value from string key
         * @param key The string representation of the enum
         * @return The corresponding enum value or null if not found
         */
        fun fromKey(key: String): MetadataType? = values().find { it.key == key }

        /**
         * The minimum cooldown period between auto-ingestion runs
         * Used to prevent concurrent processing of the same contract
         */
        val AUTO_MODE_COOLDOWN: Duration = Duration.ofMinutes(30)
    }
}