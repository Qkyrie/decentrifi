@file:OptIn(ExperimentalTime::class, ExperimentalTime::class)

package fi.decentri.dataapi.service

import fi.decentri.dataapi.repository.TransferEventRepository
import fi.decentri.dataapi.repository.TransferEventRepository.TransferData
import fi.decentri.db.DatabaseFactory.dbQuery
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CounterPartyService(private val transferEventRepository: TransferEventRepository) {

    suspend fun getTopCounterparties(
        network: String,
        safe: String,
        daysToLookBack: Int,
        limit: Int = 10
    ): List<CounterpartyData> {
        val startDate = if (daysToLookBack > 0) {
            LocalDateTime.now().minusDays(daysToLookBack.toLong()).toInstant(ZoneOffset.UTC)
        } else {
            Instant.MIN // All time
        }

        return dbQuery {
            transferEventRepository.fetchAllTransfers(network, safe, startDate)
                .groupBy { transfer ->
                    extractCounterparty(transfer, safe)
                }
                .map { (address, transactions) ->
                    CounterpartyData(
                        address = address,
                        totalVolume = transactions.sumOf { row ->
                            row.amount
                        },
                        transactionCount = transactions.size
                    )
                }
                .sortedByDescending { it.totalVolume }
                .take(limit).toList()
        }
    }

    private fun extractCounterparty(row: TransferData, contract: String): String {
        val fromAddress = row.fromAddress
        val toAddress = row.toAddress
        // The counterparty is the address that's not the contract
        return if (fromAddress.equals(contract, ignoreCase = true)) toAddress else fromAddress
    }
}

/**
 * Data class to hold counterparty data
 */
data class CounterpartyData(
    val address: String,
    val totalVolume: BigInteger,
    val transactionCount: Int
)