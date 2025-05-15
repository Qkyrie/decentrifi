package fi.decentri.dataapi.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime

/**
 * Data point representing inflow/outflow for a specific day
 */
@Serializable
data class TokenFlowPoint(
    val timestamp: Instant, // ISO-8601 format in UTC
    val inflow: Double,     // Token amount flowing in
    val outflow: Double,    // Token amount flowing out
    val netFlow: Double     // inflow - outflow
) {
    companion object {
        fun from(dateTime: LocalDateTime, inflow: Double, outflow: Double): TokenFlowPoint {
            return TokenFlowPoint(
                timestamp = dateTime.toInstant(java.time.ZoneOffset.UTC),
                inflow = inflow,
                outflow = outflow,
                netFlow = inflow - outflow
            )
        }
    }
}

/**
 * DTO for token flows data over time
 */
@Serializable
data class TokenFlowsDTO(
    val network: String,
    val contract: String,
    val period: String = "daily",
    val tokenSymbol: String = "TOKEN",
    val tokenDecimals: Int = 18,
    val dataPoints: List<TokenFlowPoint>,
    val totalInflow: Double,
    val totalOutflow: Double,
    val netFlow: Double
)