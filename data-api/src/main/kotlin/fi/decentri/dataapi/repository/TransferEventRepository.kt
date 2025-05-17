package fi.decentri.dataapi.repository

import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.token.TransferEvents
import org.jetbrains.exposed.sql.*
import java.math.BigInteger
import java.time.*
import kotlin.sequences.Sequence
import kotlin.time.ExperimentalTime

/**
 * Repository for accessing token transfer events
 */
@ExperimentalTime
class TransferEventRepository {

    /**
     * Model for transfer event data retrieved from the database
     */
    data class TransferData(
        val token: String,
        val timestamp: Instant,
        val fromAddress: String,
        val toAddress: String,
        val amount: BigInteger
    )

    /**
     * Data class to hold daily token flow data
     */
    data class DailyTokenFlow(
        val date: Instant,
        val inflow: BigInteger,
        val outflow: BigInteger
    ) {
        val netFlow: BigInteger
            get() = inflow - outflow
    }

    /**
     * Data class to hold token metadata
     */
    data class TokenMetadata(
        val symbol: String,
        val decimals: Int
    )

    /**
     * Retrieves all transfer events for a contract within a time period,
     * then transforms them using a functional approach to calculate daily flows
     */
    suspend fun getDailyTokenFlows(
        network: String,
        contract: String,
        daysToLookBack: Int
    ): Map<TokenAddress, List<DailyTokenFlow>> {
        val startDate = LocalDateTime.now().minusDays(daysToLookBack.toLong()).toInstant(ZoneOffset.UTC)

        // Fetch all transfer events in a single query
        val transferEvents = fetchAllTransfers(network, contract, startDate)

        // Group by day and aggregate using functional operations
        return calculateDailyFlows(transferEvents, contract)
    }

    /**
     * Fetches all transfer events for a contract after a certain date
     */
    private suspend fun fetchAllTransfers(network: String, contract: String, startDate: Instant): List<TransferData> {
        return dbQuery {
            TransferEvents
                .selectAll()
                .where {
                    (TransferEvents.network eq network) and
                            ((TransferEvents.fromAddress eq contract.lowercase()) or (TransferEvents.toAddress eq contract.lowercase())) and
                            (TransferEvents.blockTimestamp greaterEq startDate)
                }
                .map { row ->
                    TransferData(
                        timestamp = row[TransferEvents.blockTimestamp],
                        fromAddress = row[TransferEvents.fromAddress],
                        toAddress = row[TransferEvents.toAddress],
                        amount = row[TransferEvents.amount].toBigInteger(),
                        token = row[TransferEvents.tokenAddress]
                    )
                }
        }
    }

    @JvmInline
    value class TokenAddress(val name: String)

    /**
     * Transform the transfer events into daily flows using a functional approach
     */
    private fun calculateDailyFlows(
        transfers: List<TransferData>,
        contract: String
    ): Map<TokenAddress, List<DailyTokenFlow>> {
        // Helper function to get start of day for a timestamp
        fun getStartOfDay(instant: Instant): Instant {
            val localDate = instant.atZone(ZoneOffset.UTC).toLocalDate()
            return localDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        }

        val perToken = transfers.groupBy { it.token }

        // Group transfers by day
        return perToken.map { (token, transfersForToken) ->
            TokenAddress(token) to transfersForToken.groupBy { getStartOfDay(it.timestamp) }
                .map { (dayStart, dayTransfers) ->
                    // Calculate inflows - when tokens are sent TO the contract
                    val inflows = dayTransfers
                        .filter { it.toAddress == contract }
                        .sumOf { it.amount }

                    // Calculate outflows - when tokens are sent FROM the contract
                    val outflows = dayTransfers
                        .filter { it.fromAddress == contract }
                        .sumOf { it.amount }

                    DailyTokenFlow(
                        date = dayStart,
                        inflow = inflows,
                        outflow = outflows
                    )
                }
                .sortedBy { it.date }
        }.toMap()

    }

    suspend fun getTransferData(
        network: String,
        safe: String,
        since: Instant
    ): List<TransferData> {
        return dbQuery {
            TransferEvents
                .selectAll().where {
                    (TransferEvents.network eq network) and
                            ((TransferEvents.fromAddress eq safe.lowercase()) or (TransferEvents.toAddress eq safe.lowercase())) and
                            (TransferEvents.blockTimestamp greaterEq since)
                }.asSequence().map { row ->
                    TransferData(
                        timestamp = row[TransferEvents.blockTimestamp],
                        fromAddress = row[TransferEvents.fromAddress].lowercase(),
                        toAddress = row[TransferEvents.toAddress].lowercase(),
                        amount = row[TransferEvents.amount].toBigInteger(),
                        token = row[TransferEvents.tokenAddress]
                    )
                }.toList()
        }
    }
}

