package fi.decentri.dataapi.service

import fi.decentri.dataapi.model.TokenFlowPoint
import fi.decentri.dataapi.model.TokenFlowsDTO
import fi.decentri.dataapi.repository.TransferEventRepository
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.ExperimentalTime

/**
 * Service for handling token flow analytics
 * Uses the TransferEvents table to compute token inflows and outflows
 */
@ExperimentalTime
class TokenFlowService(private val transferEventRepository: TransferEventRepository) {

    /**
     * Gets daily token flow data for a specific contract on a given network
     * Data is extracted from the TransferEvents table for the past 30 days
     */
    suspend fun getTokenFlows(network: String, contract: String, daysToLookBack: Int = 30): TokenFlowsDTO {
        // Get token metadata for symbol and decimals
        val tokenMetadata = transferEventRepository.getTokenMetadata(network, contract.lowercase())

        // Fetch token flows from the repository 
        val dailyFlows =
            transferEventRepository.getDailyTokenFlows(network.lowercase(), contract.lowercase(), daysToLookBack)

        // Convert to data points
        val dataPoints = dailyFlows.map { flow ->
            TokenFlowPoint(
                timestamp = flow.date,
                inflow = flow.inflow,
                outflow = flow.outflow,
                netFlow = flow.netFlow
            )
        }

        // Calculate totals
        val totalInflow = dataPoints.sumOf { it.inflow }
        val totalOutflow = dataPoints.sumOf { it.outflow }
        val netFlow = totalInflow - totalOutflow

        return TokenFlowsDTO(
            network = network,
            contract = contract,
            period = "daily",
            tokenSymbol = tokenMetadata?.symbol ?: "TOKEN",
            tokenDecimals = tokenMetadata?.decimals ?: 18,
            dataPoints = dataPoints,
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            netFlow = netFlow
        )
    }
}