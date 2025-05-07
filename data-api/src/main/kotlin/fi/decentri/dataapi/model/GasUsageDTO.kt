package fi.decentri.dataapi.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime

/**
 * DTO for gas usage data points
 */
@Serializable
data class GasUsagePoint(
    val timestamp: Instant, // ISO-8601 format in UTC
    val gasUsed: Long
) {
    companion object {
        fun from(dateTime: LocalDateTime, gasUsed: Long): GasUsagePoint {
            return GasUsagePoint(
                timestamp = dateTime.toInstant(java.time.ZoneOffset.UTC),
                gasUsed = gasUsed
            )
        }
    }
}

/**
 * DTO for daily gas usage data
 */
@Serializable
data class InvocationDataDTO(
    val network: String,
    val contract: String,
    val period: String = "daily",
    val dataPoints: List<GasUsagePoint>,
    val txCount: Long,
)