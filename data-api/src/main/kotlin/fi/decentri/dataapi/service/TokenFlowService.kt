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
class TokenFlowService(
    private val transferEventRepository: TransferEventRepository,
    private val tokenService: TokenService
) {

    /**
     * Gets daily token flow data for a specific contract on a given network
     * Data is extracted from the TransferEvents table for the past 30 days
     */
    suspend fun getTokenFlows(network: String, safe: String, daysToLookBack: Int = 30): List<TokenFlowsDTO> {

        // Fetch token flows from the repository 
        val dailyFlows =
            transferEventRepository.getDailyTokenFlows(network.lowercase(), safe.lowercase(), daysToLookBack)

        return dailyFlows.map { (tokenName, dailyFlows) ->
            val token = tokenService.getToken(network, tokenName.name)
            val dataPoints = dailyFlows.map { flow ->
                TokenFlowPoint(
                    timestamp = flow.date,
                    inflow = flow.inflow.asEth(6).toDouble(),
                    outflow = flow.outflow.asEth(6).toDouble(),
                    netFlow = flow.netFlow.asEth(6).toDouble()
                )
            }

            // Calculate totals
            val totalInflow = dataPoints.sumOf { it.inflow }
            val totalOutflow = dataPoints.sumOf { it.outflow }
            val netFlow = totalInflow - totalOutflow

            TokenFlowsDTO(
                network = network,
                period = "daily",
                tokenSymbol = token!!.name,
                tokenDecimals = token.decimals,
                dataPoints = dataPoints,
                totalInflow = totalInflow,
                totalOutflow = totalOutflow,
                netFlow = netFlow
            )
        }
    }
}