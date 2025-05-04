package fi.decentri.dataapi.model

import java.time.LocalDateTime

/**
 * DTO for gas usage data points
 */
data class GasUsagePoint(
    val timestamp: LocalDateTime,
    val gasUsed: Long
)

/**
 * DTO for daily gas usage data
 */
data class DailyGasUsageDTO(
    val network: String,
    val contract: String,
    val period: String = "daily",
    val dataPoints: List<GasUsagePoint>
)