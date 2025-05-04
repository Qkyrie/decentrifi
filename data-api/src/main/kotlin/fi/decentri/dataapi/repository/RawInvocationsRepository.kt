package fi.decentri.dataapi.repository

import fi.decentri.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.hour
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Table definition for raw_invocations
 */
object RawInvocations : Table("raw_invocations") {
    val id = integer("id").autoIncrement()
    val network = varchar("network", 50)
    val contractAddress = varchar("contract_address", 50)
    val blockNumber = long("block_number")
    val blockTimestamp = timestamp("block_timestamp")
    val txHash = varchar("tx_hash", 66)
    val fromAddress = varchar("from_address", 50)
    val functionSelector = varchar("function_selector", 10).nullable()
    val status = bool("status")
    val gasUsed = long("gas_used")

    override val primaryKey = PrimaryKey(id)
}

/**
 * Repository for accessing raw invocations data
 */
class RawInvocationsRepository {
    /**
     * Get gas usage data aggregated by hour for the last 24 hours
     */
    suspend fun getGasUsageByHourLast24Hours(network: String, contract: String): List<Pair<LocalDateTime, Long>> =
        dbQuery {
            val now = LocalDateTime.now(ZoneOffset.UTC)
            val oneDayAgo = now.minusDays(1)

            RawInvocations.slice(
                RawInvocations.blockTimestamp.hour().alias("hour"),
                RawInvocations.gasUsed.sum().alias("gas_used")
            )
                .selectAll().where {
                    (RawInvocations.network eq network) and
                            (RawInvocations.contractAddress eq contract) and
                            (RawInvocations.blockTimestamp greaterEq oneDayAgo.toInstant(ZoneOffset.UTC)) and
                            (RawInvocations.blockTimestamp lessEq now.toInstant(ZoneOffset.UTC))
                }
                .groupBy(RawInvocations.blockTimestamp.hour())
                .orderBy(RawInvocations.blockTimestamp.hour())
                .map { row ->
                    // Create a datetime for the given hour
                    val hour = row[RawInvocations.blockTimestamp.hour().alias("hour")]
                    // Use current hour as base for new time
                    val hourDateTime = LocalDateTime.of(now.year, now.month, now.dayOfMonth, hour, 0)

                    // If the hour is after current hour, it means it's from yesterday
                    val adjustedDateTime = if (hour > now.hour) hourDateTime.minusDays(1) else hourDateTime

                    // Sum of gas used for this hour
                    val gasUsed = row[RawInvocations.gasUsed.sum().alias("gas_used")] ?: 0

                    Pair(adjustedDateTime, gasUsed)
                }
        }
}