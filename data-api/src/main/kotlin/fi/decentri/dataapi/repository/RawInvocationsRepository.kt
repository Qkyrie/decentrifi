package fi.decentri.dataapi.repository

import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.rawinvocation.RawInvocations
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.countDistinct
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.lowerCase
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit


/**
 * Repository for accessing raw invocations data
 */
class RawInvocationsRepository {

    /**
     * Get all raw gas usage entries for a contract on a network that are older than 24 hours
     * Returns all entries without any grouping, to allow for custom aggregation in code
     */
    suspend fun getRawGasUsageOlderThan24Hours(network: String, contract: String): List<Pair<LocalDateTime, Long>> =
        dbQuery {
            val now = Instant.now()
            val oneDayAgo = now.minus(24, ChronoUnit.HOURS)

            RawInvocations
                .slice(RawInvocations.blockTimestamp, RawInvocations.gasUsed)
                .selectAll().where {
                    (RawInvocations.network eq network) and
                            (RawInvocations.contractAddress.lowerCase() eq contract.lowercase()) and
                            (RawInvocations.blockTimestamp greaterEq oneDayAgo)
                }
                .orderBy(RawInvocations.blockTimestamp)
                .map { row ->
                    Pair(
                        row[RawInvocations.blockTimestamp].atOffset(ZoneOffset.UTC).toLocalDateTime(),
                        row[RawInvocations.gasUsed]
                    )
                }
        }
        
    /**
     * Count the number of unique from_addresses for a contract on a network in the last 24 hours
     * Returns the total count of distinct addresses
     */
    suspend fun getUniqueFromAddressCount24Hours(network: String, contract: String): Long =
        dbQuery {
            val now = LocalDateTime.now(ZoneOffset.UTC)
            val oneDayAgo = now.minusDays(1)

            RawInvocations
                .slice(RawInvocations.fromAddress.countDistinct())
                .selectAll().where {
                    (RawInvocations.network eq network) and
                            (RawInvocations.contractAddress.lowerCase() eq contract.lowercase()) and
                            (RawInvocations.blockTimestamp greaterEq oneDayAgo.toInstant(ZoneOffset.UTC))
                }
                .first()[RawInvocations.fromAddress.countDistinct()]
        }
}