package fi.decentri.dataapi.service

import fi.decentri.dataapi.model.InvocationDataDTO
import fi.decentri.dataapi.model.GasUsagePoint
import fi.decentri.dataapi.model.UniqueAddressesDTO
import fi.decentri.dataapi.repository.RawInvocationsRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Service for handling gas usage data and metrics
 */
class GasUsageService(private val rawInvocationsRepository: RawInvocationsRepository) {

    /**
     * Gets daily gas usage for a specific contract on a given network
     * This version fetches raw gas usage data and groups it by hour in code
     */
    suspend fun getInvocationData(network: String, contract: String): InvocationDataDTO {
        // Get all raw data points from the repository
        val rawDataPoints = rawInvocationsRepository.getRawGasUsageOlderThan24Hours(network, contract)

        // Group by hour and calculate sum for each hour
        val gasUsageByHour = rawDataPoints
            .groupBy { it.first.toLocalDate().atTime(it.first.hour, 0) }
            .mapValues { entry -> entry.value.sumOf { it.second } }
            .map { (timestamp, gasUsed) -> GasUsagePoint.from(timestamp, gasUsed) }

        // Fill in missing hours with zero values to ensure a complete 24-hour dataset
        val filledDataPoints = fillMissingHours(gasUsageByHour)

        return InvocationDataDTO(
            network = network,
            contract = contract,
            dataPoints = filledDataPoints,
            txCount = rawDataPoints.size.toLong()
        )
    }

    /**
     * Gets the count of unique from_addresses that have interacted with the contract
     * in the last 24 hours
     */
    suspend fun getUniqueAddressesCount(network: String, contract: String, since: LocalDateTime): UniqueAddressesDTO {
        val uniqueCount = rawInvocationsRepository.getUniqueFromAddressCount24Hours(network, contract, since)
        return UniqueAddressesDTO(
            network = network,
            contract = contract,
            uniqueAddressCount = uniqueCount,
            periodHours = 24
        )
    }

    /**
     * Helper method to ensure we have data points for all 24 hours
     * If data is missing for an hour, we fill with a zero value
     */
    private fun fillMissingHours(points: List<GasUsagePoint>): List<GasUsagePoint> {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val oneDayAgo = now.minusHours(23).withMinute(0).withSecond(0).withNano(0)

        val existingPointsMap = mutableMapOf<LocalDateTime, GasUsagePoint>()

        // Create map of existing points by parsed timestamp
        points.forEach { point ->
            try {
                val pointTime = point.timestamp.atOffset(ZoneOffset.UTC)
                    .withMinute(0).withSecond(0).withNano(0).toLocalDateTime()
                existingPointsMap[pointTime] = point
            } catch (e: Exception) {
                // Skip points with invalid timestamps
            }
        }

        // Generate all hours for the past 24 hours
        val result = mutableListOf<GasUsagePoint>()

        // Create a data point for each hour in the 24-hour range
        for (i in 0 until 24) {
            val hourTimestamp = oneDayAgo.plusHours(i.toLong())
            // Look for existing data point with this timestamp
            val existingPoint = existingPointsMap[hourTimestamp]

            // Use existing point if available, otherwise create a zero point
            if (existingPoint != null) {
                result.add(existingPoint)
            } else {
                result.add(
                    GasUsagePoint.from(
                        dateTime = hourTimestamp,
                        gasUsed = BigDecimal.ZERO
                    )
                )
            }
        }

        return result.sortedBy { it.timestamp }
    }
}