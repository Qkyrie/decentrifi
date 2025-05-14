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
    LAST_PROCESSED_BLOCK_TRANSFER_EVENTS("last_processed_block_transfer_events"),
    EVENTS_LAST_RUN_TIMESTAMP("events_last_run_timestamp"),
    RAW_INVOCATIONS_LAST_RUN_TIMESTAMP("raw_invocations_last_run_timestamp"),
    TRANSFER_EVENTS_LAST_RUN_TIMESTAMP("transfer_events_last_run_timestamp");

    companion object {
        /**
         * The minimum cooldown period between auto-ingestion runs
         * Used to prevent concurrent processing of the same contract
         */
        val AUTO_MODE_COOLDOWN: Duration = Duration.ofMinutes(30)
    }
}