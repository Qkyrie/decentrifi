package fi.decentri.dataapi.model

import kotlinx.serialization.Serializable

/**
 * Data point representing a counterparty and their transaction volume
 */
@Serializable
data class Counterparty(
    val address: String,      // Ethereum address
    val shortAddress: String, // e.g., "0x42a9...3c82"
    val volume: Double,       // Total token volume
    val transactionCount: Int,// Number of transactions
    val percentage: Double    // Percentage of total volume
)

/**
 * DTO for top counterparties data
 */
@Serializable
data class TopCounterpartiesDTO(
    val network: String,
    val contract: String,
    val period: String = "all",
    val tokenSymbol: String = "TOKEN",
    val tokenDecimals: Int = 18,
    val totalVolume: Double,
    val counterparties: List<Counterparty>
)