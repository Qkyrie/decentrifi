package fi.decentri.dataingest.model

/**
 * Enum representing the types of metadata tracked during ingestion
 * This helps prevent errors from typos in string literals
 */
enum class MetadataType(val key: String) {
    LAST_PROCESSED_BLOCK_RAW_INVOCATIONS("last_processed_block_raw_invocations"),
    LAST_PROCESSED_BLOCK_EVENTS("last_processed_block_events"),
    EVENTS_LAST_RUN_TIMESTAMP("events_last_run_timestamp");

    companion object {
        /**
         * Get enum value from string key
         * @param key The string representation of the enum
         * @return The corresponding enum value or null if not found
         */
        fun fromKey(key: String): MetadataType? = values().find { it.key == key }
    }
}