package fi.decentri.dataapi.service

import fi.decentri.dataapi.FormatUtilsExtensions.asEth
import fi.decentri.dataapi.model.TokenFlowPoint
import fi.decentri.dataapi.model.TokenFlowsDTO
import fi.decentri.dataapi.repository.TransferEventRepository
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
    suspend fun getTokenFlows(network: String, safe: String, daysToLookBack: Int = 30): List<TokenFlowsDTO> {

        // Fetch token flows from the repository 
        val dailyFlows =
            transferEventRepository.getDailyTokenFlows(network.lowercase(), safe.lowercase(), daysToLookBack)

        return dailyFlows.map { (tokenName, dailyFlows) ->
            //Todo: fetch token here
            val dataPoints = dailyFlows.map { flow ->
                TokenFlowPoint(
                    timestamp = flow.date,
                    inflow = flow.inflow.asEth().toDouble(),
                    outflow = flow.outflow.asEth().toDouble(),
                    netFlow = flow.netFlow.asEth().toDouble()
                )
            }

            // Calculate totals
            val totalInflow = dataPoints.sumOf { it.inflow }
            val totalOutflow = dataPoints.sumOf { it.outflow }
            val netFlow = totalInflow - totalOutflow

            TokenFlowsDTO(
                network = network,
                contract = safe,
                period = "daily",
                tokenSymbol = tokenName.name,
                tokenDecimals = 18,
                dataPoints = dataPoints,
                totalInflow = totalInflow,
                totalOutflow = totalOutflow,
                netFlow = netFlow
            )
        }
    }
}