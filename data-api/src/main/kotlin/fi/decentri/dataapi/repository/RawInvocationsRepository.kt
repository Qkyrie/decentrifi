package fi.decentri.dataapi.repository

import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.rawinvocation.RawInvocations
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime
import java.time.ZoneOffset


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
            val now = LocalDateTime.now(ZoneOffset.UTC)
            val oneDayAgo = now.minusDays(1)

            RawInvocations
                .slice(RawInvocations.blockTimestamp, RawInvocations.gasUsed)
                .selectAll().where {
                    (RawInvocations.network eq network) and
                            (RawInvocations.contractAddress eq contract) and
                            (RawInvocations.blockTimestamp greaterEq oneDayAgo.toInstant(ZoneOffset.UTC))
                }
                .orderBy(RawInvocations.blockTimestamp)
                .map { row ->
                    Pair(
                        row[RawInvocations.blockTimestamp].atOffset(ZoneOffset.UTC).toLocalDateTime(),
                        row[RawInvocations.gasUsed]
                    )
                }
        }
}