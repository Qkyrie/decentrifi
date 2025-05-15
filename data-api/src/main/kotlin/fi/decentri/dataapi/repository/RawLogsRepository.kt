package fi.decentri.dataapi.repository

import fi.decentri.dataapi.routes.JsonbFilter
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.event.RawLogs
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Repository for accessing raw logs (blockchain events) data
 */
class RawLogsRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Extension to extract text from a jsonb column (decoded ->> 'key').
     */
    fun Column<JsonElement>.jsonbText(key: String): Expression<String> =
        CustomFunction("jsonb_extract_path_text", TextColumnType(), this, stringLiteral(key))

    /**
     * Helper to fold a base predicate with a list of JsonbFilter expressions.
     */
    private fun Op<Boolean>.withJsonbFilters(
        filters: List<JsonbFilter>,
        jsonColumn: Column<JsonElement>
    ): Op<Boolean> =
        filters.fold(this) { acc, filter ->
            val field = jsonColumn.jsonbText(filter.key)
            val cond: Op<Boolean> = field.lowerCase() like filter.value.lowercase()
            acc and cond
        }

    /**
     * Get all events for a contract on a specific network from the last 24 hours
     */
    suspend fun getEventsFromLast24Hours(
        network: String, contractAddress: String,
        filters: List<JsonbFilter> = emptyList()        // default = no filter
    ): List<RawLogEntry> =
        dbQuery {
            val now = Instant.now()
            val oneDayAgo = now.minus(24, ChronoUnit.HOURS)


            // Base predicate (network, contract, and time‑range)
            var predicate: Op<Boolean> =
                (RawLogs.network eq network.lowercase()) and
                        (RawLogs.contractAddress eq contractAddress.lowercase()) and
                        (RawLogs.blockTimestamp greaterEq oneDayAgo)

            // Augment with JSONB filters, if provided
            if (filters.isNotEmpty()) {
                predicate = predicate.withJsonbFilters(filters, RawLogs.decoded)
            }

            RawLogs
                .selectAll().where(predicate)
                .orderBy(RawLogs.blockTimestamp)
                .map { row ->
                    RawLogEntry(
                        contractAddress = row[RawLogs.contractAddress],
                        txHash = row[RawLogs.txHash],
                        logIndex = row[RawLogs.logIndex],
                        blockNumber = row[RawLogs.blockNumber],
                        blockTimestamp = row[RawLogs.blockTimestamp],
                        eventName = row[RawLogs.eventName],
                        topics = row[RawLogs.topics],
                        data = row[RawLogs.data],
                        decoded = row[RawLogs.decoded]
                    )
                }
        }

    /**
     * Get all unique event names and their decoded parameter keys for a specific contract
     */
    suspend fun getDecodedEventKeys(network: String, contractAddress: String): Map<String, List<String>> =
        dbQuery {
            logger.info("Getting decoded event keys for network=$network, contract=$contractAddress")

            val predicate: Op<Boolean> =
                (RawLogs.network eq network.lowercase()) and
                        (RawLogs.contractAddress eq contractAddress.lowercase())

            // Find distinct event names first
            val events = RawLogs
                .slice(RawLogs.eventName)
                .select(predicate)
                .groupBy(RawLogs.eventName)
                .mapNotNull { it[RawLogs.eventName] }
                .filterNot { it.isNullOrEmpty() }
                .toSet()

            logger.info("Found ${events.size} distinct event types")

            // Find a sample of each event to extract keys
            val result = mutableMapOf<String, List<String>>()

            events.forEach { eventName ->
                // Get a sample of this event type to extract keys
                val sampleEvent = RawLogs
                    .select((predicate) and (RawLogs.eventName eq eventName))
                    .limit(1)
                    .map { row ->
                        RawLogEntry(
                            contractAddress = row[RawLogs.contractAddress],
                            txHash = row[RawLogs.txHash],
                            logIndex = row[RawLogs.logIndex],
                            blockNumber = row[RawLogs.blockNumber],
                            blockTimestamp = row[RawLogs.blockTimestamp],
                            eventName = row[RawLogs.eventName],
                            topics = row[RawLogs.topics],
                            data = row[RawLogs.data],
                            decoded = row[RawLogs.decoded]
                        )
                    }
                    .firstOrNull()

                if (sampleEvent != null) {
                    // Extract keys from the sample event
                    try {
                        val keys = sampleEvent.decoded.jsonObject.keys.toList()
                        result[eventName] = keys
                        logger.info("Found ${keys.size} keys for event $eventName")
                    } catch (e: Exception) {
                        logger.warn("Failed to extract keys for event $eventName: ${e.message}")
                        result[eventName] = emptyList()
                    }
                }
            }

            result
        }
}

/**
 * Internal data class for raw log entries from the database
 */
data class RawLogEntry(
    val contractAddress: String,
    val txHash: String,
    val logIndex: Int,
    val blockNumber: Long,
    val blockTimestamp: Instant,
    val eventName: String?,
    val topics: JsonElement,
    val data: String?,
    val decoded: JsonElement
)