package fi.decentri.dataapi.service

import fi.decentri.dataapi.model.DailyGasUsageDTO
import fi.decentri.dataapi.model.GasUsagePoint
import fi.decentri.dataapi.repository.RawInvocationsRepository
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Service for handling gas usage data and metrics
 */
class GasUsageService(private val rawInvocationsRepository: RawInvocationsRepository) {
    
    /**
     * Gets daily gas usage for a specific contract on a given network
     */
    suspend fun getDailyGasUsage(network: String, contract: String): DailyGasUsageDTO {
        // Get the raw data points from the repository
        val dataPoints = rawInvocationsRepository.getGasUsageByHourLast24Hours(network, contract)
        
        // Convert to the DTO format
        val gasUsagePoints = dataPoints.map { (timestamp, gasUsed) ->
            GasUsagePoint(timestamp, gasUsed)
        }
        
        // Fill in missing hours with zero values to ensure a complete 24-hour dataset
        val filledDataPoints = fillMissingHours(gasUsagePoints)
        
        return DailyGasUsageDTO(
            network = network,
            contract = contract,
            dataPoints = filledDataPoints
        )
    }
    
    /**
     * Helper method to ensure we have data points for all 24 hours
     * If data is missing for an hour, we fill with a zero value
     */
    private fun fillMissingHours(points: List<GasUsagePoint>): List<GasUsagePoint> {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val oneDayAgo = now.minusDays(1)
        
        // Create a map of existing points by hour
        val pointsByHour = points.associateBy { it.timestamp.hour }
        
        // Generate all hours for the past 24 hours
        return (0..23).map { hourOffset ->
            val hourTime = oneDayAgo.plusHours(hourOffset.toLong())
            val hour = hourTime.hour
            
            // Use existing point if available, otherwise create a zero point
            pointsByHour[hour] ?: GasUsagePoint(
                timestamp = LocalDateTime.of(hourTime.year, hourTime.month, hourTime.dayOfMonth, hour, 0),
                gasUsed = 0
            )
        }.sortedBy { it.timestamp }
    }
}