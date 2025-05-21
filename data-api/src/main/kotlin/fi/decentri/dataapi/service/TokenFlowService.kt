package fi.decentri.dataapi.service

import fi.decentri.evm.FormatUtilsExtensions.asEth
import fi.decentri.dataapi.model.TokenFlowPoint
import fi.decentri.dataapi.model.TokenFlowsDTO
import fi.decentri.dataapi.repository.TransferEventRepository
import fi.decentri.dataapi.repository.TransferEventRepository.*
import java.time.Instant
import java.time.LocalDateTime
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

    suspend fun getTokenFlows(network: String, safe: String, daysSince: Int = 365): List<TokenFlowsDTO> {

        // Fetch token flows from the repository 
        val dailyFlows = getDailyFlows(network, safe, daysSince)

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

    @JvmInline
    value class TokenAddress(val name: String)

    private suspend fun getDailyFlows(
        network: String,
        safe: String,
        daysSince: Int
    ): Map<TokenAddress, List<DailyTokenFlow>> {
        val startDate = LocalDateTime.now().minusDays(daysSince.toLong()).toInstant(ZoneOffset.UTC)
        val transferEvents = transferEventRepository.fetchAllTransfers(network, safe, startDate)
        // Group by day and aggregate using functional operations
        return calculateDailyFlows(transferEvents, safe)
    }

    /**
     * Transform the transfer events into daily flows using a functional approach
     */
    private fun calculateDailyFlows(
        transfers: List<TransferData>,
        contract: String
    ): Map<TokenAddress, List<DailyTokenFlow>> {
        // Helper function to get start of day for a timestamp
        fun getStartOfDay(instant: Instant): Instant {
            val localDate = instant.atZone(ZoneOffset.UTC).toLocalDate()
            return localDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        }

        val perToken = transfers.groupBy { it.token }

        // Group transfers by day
        return perToken.map { (token, transfersForToken) ->
            TokenAddress(token) to transfersForToken.groupBy { getStartOfDay(it.timestamp) }
                .map { (dayStart, dayTransfers) ->
                    // Calculate inflows - when tokens are sent TO the contract
                    val inflows = dayTransfers
                        .filter { it.toAddress == contract }
                        .sumOf { it.amount }

                    // Calculate outflows - when tokens are sent FROM the contract
                    val outflows = dayTransfers
                        .filter { it.fromAddress == contract }
                        .sumOf { it.amount }

                    DailyTokenFlow(
                        date = dayStart,
                        inflow = inflows,
                        outflow = outflows
                    )
                }
                .sortedBy { it.date }
        }.toMap()

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