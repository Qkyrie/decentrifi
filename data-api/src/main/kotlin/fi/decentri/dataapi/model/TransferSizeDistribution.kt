package fi.decentri.dataapi.model

import kotlinx.serialization.Serializable

/**
 * Data point representing a size range and the count of transfers in that range
 */
@Serializable
data class TransferSizeRange(
    val range: String,        // e.g., "0-10", "10-100", "100-1k", "1k-10k", etc.
    val count: Int,           // Number of transfers in this range
    val percentage: Double    // Percentage of total transfers
)

/**
 * DTO for transfer size distribution data
 */
@Serializable
data class TransferSizeDistributionDTO(
    val network: String,
    val contract: String,
    val period: String = "all",
    val tokenSymbol: String = "TOKEN",
    val totalTransfers: Int,
    val distribution: List<TransferSizeRange>
)