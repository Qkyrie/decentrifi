package fi.decentri.dataapi.model

import java.math.BigDecimal
import java.time.Instant
import kotlin.time.ExperimentalTime

/**
 * Data class representing a token transfer event
 */
@ExperimentalTime
data class TransferEvent(
    val id: Int? = null,
    val network: String,
    val tokenAddress: String,
    val contractAddress: String? = null, 
    val txHash: String,
    val logIndex: Int,
    val blockNumber: Long,
    val blockTimestamp: Instant,
    val fromAddress: String,
    val toAddress: String,
    val amount: BigDecimal,
    val tokenSymbol: String? = null,
    val tokenDecimals: Int? = null
)