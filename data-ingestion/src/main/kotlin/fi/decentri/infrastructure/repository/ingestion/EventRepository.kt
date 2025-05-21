package fi.decentri.infrastructure.repository.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.event.RawLogs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.jetbrains.exposed.sql.batchInsert
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Repository for managing blockchain event logs
 */
class EventRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapper()

    /**
     * Batch insert multiple event log records
     */
    suspend fun batchInsertEvents(events: List<EventLogData>) {
        if (events.isEmpty()) return

        logger.debug("Batch inserting ${events.size} event logs")
        dbQuery {
            RawLogs.batchInsert(
                data = events,
                ignore = true) { data ->
                this[RawLogs.network] = data.network
                this[RawLogs.contractAddress] = data.contractAddress
                this[RawLogs.txHash] = data.txHash
                this[RawLogs.logIndex] = data.logIndex
                this[RawLogs.blockNumber] = data.blockNumber
                this[RawLogs.blockTimestamp] = data.blockTimestamp
                this[RawLogs.topic0] = data.topic0
                this[RawLogs.topics] = Json.encodeToJsonElement(data.topics)
                this[RawLogs.data] = data.data
                this[RawLogs.eventName] = data.eventName
                this[RawLogs.decoded] = Json.encodeToJsonElement(data.decoded)
            }
        }
    }
}

/**
 * Data class for event log batch operations
 */
data class EventLogData(
    val network: String,
    val contractAddress: String,
    val txHash: String,
    val logIndex: Int,
    val blockNumber: Long,
    val blockTimestamp: Instant,
    val topic0: String?,
    val topics: List<String>,
    val data: String?,
    val eventName: String?,
    val decoded: Map<String, Any>?
)