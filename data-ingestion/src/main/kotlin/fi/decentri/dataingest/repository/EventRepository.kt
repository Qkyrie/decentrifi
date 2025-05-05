package fi.decentri.dataingest.repository

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.event.EventDefinitions
import fi.decentri.db.event.RawLogs
import org.jetbrains.exposed.sql.*
import org.postgresql.util.PGobject
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
            RawLogs.batchInsert(events) { data ->
                this[RawLogs.network] = data.network
                this[RawLogs.contractAddress] = data.contractAddress
                this[RawLogs.txHash] = data.txHash
                this[RawLogs.logIndex] = data.logIndex
                this[RawLogs.blockNumber] = data.blockNumber
                this[RawLogs.blockTimestamp] = data.blockTimestamp
                this[RawLogs.topic0] = data.topic0
                this[RawLogs.topics] = data.topics.toTypedArray().toList()
                this[RawLogs.data] = data.data
                this[RawLogs.eventName] = data.eventName
                
                // Convert decoded data to JSONB for storage
                this[RawLogs.decoded] = data.decoded?.let {
                    PGobject().apply {
                        type = "jsonb"
                        value = objectMapper.writeValueAsString(it)
                    }
                }
            }
        }
    }

    /**
     * Store an event definition from ABI
     */
    suspend fun storeEventDefinition(definition: EventDefinitionData) {
        logger.debug("Storing event definition: ${definition.eventName} for contract ${definition.contractAddress}")
        dbQuery {
            // Check if this event definition already exists
            val existing = EventDefinitions.selectAll().where {
                (EventDefinitions.contractAddress eq definition.contractAddress) and
                (EventDefinitions.signature eq definition.signature) and
                (EventDefinitions.network eq definition.network)
            }.firstOrNull()

            if (existing == null) {
                // Insert new event definition
                EventDefinitions.insert {
                    it[contractAddress] = definition.contractAddress
                    it[eventName] = definition.eventName
                    it[signature] = definition.signature
                    it[abiJson] = definition.abiJson
                    it[network] = definition.network
                    it[createdAt] = Instant.now()
                    it[updatedAt] = Instant.now()
                }
            }
        }
    }

    /**
     * Batch insert multiple event definitions
     */
    suspend fun batchInsertEventDefinitions(definitions: List<EventDefinitionData>) {
        if (definitions.isEmpty()) return

        logger.debug("Batch inserting ${definitions.size} event definitions")
        dbQuery {
            // For simplicity, we'll process them one by one to handle the existing check
            definitions.forEach { definition ->
                // Check if this event definition already exists
                val existing = EventDefinitions.select {
                    (EventDefinitions.contractAddress eq definition.contractAddress) and
                    (EventDefinitions.signature eq definition.signature) and
                    (EventDefinitions.network eq definition.network)
                }.firstOrNull()

                if (existing == null) {
                    // Insert new event definition
                    EventDefinitions.insert {
                        it[contractAddress] = definition.contractAddress
                        it[eventName] = definition.eventName
                        it[signature] = definition.signature
                        it[abiJson] = definition.abiJson
                        it[network] = definition.network
                        it[createdAt] = Instant.now()
                        it[updatedAt] = Instant.now()
                    }
                }
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

/**
 * Data class for event definition operations
 */
data class EventDefinitionData(
    val contractAddress: String,
    val eventName: String,
    val signature: String,
    val abiJson: String,
    val network: String
)