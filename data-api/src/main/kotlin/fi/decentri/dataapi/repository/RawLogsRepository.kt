package fi.decentri.dataapi.repository

import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.event.RawLogs
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Repository for accessing raw logs (blockchain events) data
 */
class RawLogsRepository {

    /**
     * Get all events for a contract on a specific network from the last 24 hours
     */
    suspend fun getEventsFromLast24Hours(network: String, contractAddress: String): List<RawLogEntry> = 
        dbQuery {
            val now = Instant.now()
            val oneDayAgo = now.minus(24, ChronoUnit.HOURS)

            RawLogs
                .selectAll()
                .where {
                    (RawLogs.network eq network) and
                    (RawLogs.contractAddress eq contractAddress) and
                    (RawLogs.blockTimestamp greaterEq oneDayAgo)
                }
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