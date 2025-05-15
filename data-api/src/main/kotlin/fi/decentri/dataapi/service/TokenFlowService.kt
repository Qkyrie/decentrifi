package fi.decentri.dataapi.service

import fi.decentri.dataapi.FormatUtilsExtensions.asEth
import fi.decentri.dataapi.model.TokenFlowPoint
import fi.decentri.dataapi.model.TokenFlowsDTO
import fi.decentri.dataapi.repository.TransferEventRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.time.ExperimentalTime

/**
 * Service for handling token flow analytics
 * Uses the TransferEvents table to compute token inflows and outflows
 */
@ExperimentalTime
class TokenFlowService(
    private val transferEventRepository: TransferEventRepository,
    private val tokenService: TokenService
) {

    /**
     * Gets daily token flow data for a specific contract on a given network
     * Data is extracted from the TransferEvents table for the past X days
     * Ensures that every day in the requested period has a data point
     * 
     * @param network The blockchain network name
     * @param safe The contract address
     * @param daysSince Number of days to look back (default 365 days)
     * @return List of TokenFlowsDTO for each token associated with the contract
     */
    suspend fun getTokenFlows(network: String, safe: String, daysSince: Int = 365): List<TokenFlowsDTO> {

        // Fetch token flows from the repository 
        val dailyFlows =
            transferEventRepository.getDailyTokenFlows(network.lowercase(), safe.lowercase(), daysSince)

        return dailyFlows.map { (tokenName, dailyFlows) ->
            val token = tokenService.getToken(network, tokenName.name)

            // Create a map of existing data points keyed by day start
            val existingDataPoints = dailyFlows.associate {
                it.date to TokenFlowPoint(
                    timestamp = it.date,
                    inflow = it.inflow.asEth(6).toDouble(),
                    outflow = it.outflow.asEth(6).toDouble(),
                    netFlow = it.netFlow.asEth(6).toDouble()
                )
            }

            // Generate all days in the period
            val startInstant = getStartOfDayInstant(Instant.now().minus(daysSince.toLong(), ChronoUnit.DAYS))
            val endInstant = getStartOfDayInstant(Instant.now())

            // Create a complete list of data points with zeros for missing days
            val completeDataPoints = generateDaysBetween(startInstant, endInstant).map { dayInstant ->
                existingDataPoints.getOrElse(dayInstant) {
                    TokenFlowPoint(
                        timestamp = dayInstant,
                        inflow = 0.0,
                        outflow = 0.0,
                        netFlow = 0.0
                    )
                }
            }.sortedBy { it.timestamp }

            // Calculate totals
            val totalInflow = completeDataPoints.sumOf { it.inflow }
            val totalOutflow = completeDataPoints.sumOf { it.outflow }
            val netFlow = totalInflow - totalOutflow

            // Determine the appropriate period string based on daysSince
            val period = when {
                daysSince <= 30 -> "daily"
                daysSince <= 90 -> "weekly"
                else -> "monthly"
            }

            TokenFlowsDTO(
                network = network,
                period = period,
                tokenSymbol = token?.name ?: "TOKEN",
                tokenDecimals = token?.decimals ?: 18,
                dataPoints = completeDataPoints.toList(),
                totalInflow = totalInflow,
                totalOutflow = totalOutflow,
                netFlow = netFlow
            )
        }
    }

    /**
     * Converts an Instant to the start of day in UTC
     */
    private fun getStartOfDayInstant(instant: Instant): Instant {
        val localDate = instant.atZone(ZoneOffset.UTC).toLocalDate()
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant()
    }

    /**
     * Generates a sequence of all day-start instants between start and end (inclusive)
     */
    private fun generateDaysBetween(start: Instant, end: Instant): Sequence<Instant> {
        val startDate = start.atZone(ZoneOffset.UTC).toLocalDate()
        val endDate = end.atZone(ZoneOffset.UTC).toLocalDate()

        return generateSequence(startDate) { date ->
            val next = date.plusDays(1)
            if (next.isAfter(endDate)) null else next
        }.map { it.atStartOfDay(ZoneOffset.UTC).toInstant() }
    }
}