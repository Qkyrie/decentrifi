package fi.decentri.dataapi.model

enum class MetadataType(val key: String) {
    LAST_PROCESSED_BLOCK_RAW_INVOCATIONS("last_processed_block_raw_invocations"),
    LAST_PROCESSED_BLOCK_EVENTS("last_processed_block_events"),
    EVENTS_LAST_RUN_TIMESTAMP("events_last_run_timestamp")
}