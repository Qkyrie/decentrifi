package fi.decentri.dataapi.repository


import fi.decentri.db.DatabaseFactory
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.rawinvocation.RawInvocations
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import java.math.BigDecimal
import java.time.*
import java.time.temporal.ChronoUnit

/**
 * Read-only access to raw invocation metrics.
 */
class RawInvocationsRepository {

    /**
     * Gas-usage samples **older than 24 h** for a contract on a network.
     *
     * @return `(timestampUTC, gasUsedWei)` pairs ordered by time.
     */
    suspend fun getRawGasUsageOlderThan24Hours(
        network: String,
        contract: String
    ): List<Pair<LocalDateTime, BigDecimal>> {
        return dbQuery {
            val cutoff = Instant.now().minus(24, ChronoUnit.HOURS)

            RawInvocations
                .select(RawInvocations.blockTimestamp, RawInvocations.gasUsed)
                .where {
                    (RawInvocations.network eq network) and
                            (RawInvocations.contractAddr.lowerCase() eq contract.lowercase()) and
                            (RawInvocations.blockTimestamp less cutoff)
                }
                .orderBy(RawInvocations.blockTimestamp, SortOrder.ASC)
                .map { row ->
                    val tsUtc = row[RawInvocations.blockTimestamp]
                        .atOffset(ZoneOffset.UTC)
                        .toLocalDateTime()

                    Pair(tsUtc, row[RawInvocations.gasUsed])
                }
        }
    }

    /**
     * Count distinct **sender** addresses (`from_addr`) since `since` (UTC).
     */
    suspend fun getUniqueFromAddressCount24Hours(
        network: String,
        contract: String,
        since: LocalDateTime
    ): Long {
        return dbQuery {
            RawInvocations
                .select(RawInvocations.fromAddr.countDistinct())
                .where {
                    (RawInvocations.network eq network) and
                            (RawInvocations.contractAddr.lowerCase() eq contract.lowercase()) and
                            (RawInvocations.blockTimestamp greaterEq since.toInstant(ZoneOffset.UTC))
                }
                .first()[RawInvocations.fromAddr.countDistinct()]
        }

    }
}