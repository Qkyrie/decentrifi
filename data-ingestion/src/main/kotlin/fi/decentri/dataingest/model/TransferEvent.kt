package fi.decentri.dataingest.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Model class representing a token transfer event
 */
data class TransferEvent(
    val id: Int? = null,
    val network: String,
    val tokenAddress: String,
    val contractAddress: String, // Optional monitored contract address
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