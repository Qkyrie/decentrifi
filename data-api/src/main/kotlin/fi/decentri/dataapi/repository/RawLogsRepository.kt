package fi.decentri.dataapi.repository

import fi.decentri.dataapi.JsonbFilter
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.event.RawLogs
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Repository for accessing raw logs (blockchain events) data
 */
class RawLogsRepository {


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