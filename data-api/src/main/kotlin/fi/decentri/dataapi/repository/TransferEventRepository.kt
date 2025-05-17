package fi.decentri.dataapi.repository

import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.token.TransferEvents
import org.jetbrains.exposed.sql.*
import java.math.BigInteger
import java.time.*
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


    suspend fun fetchAllTransfers(network: String, contract: String, startDate: Instant): List<TransferData> {
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
}

