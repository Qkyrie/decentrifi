package fi.decentri.dataapi.routes

import fi.decentri.dataapi.model.TokenFlowPoint
import fi.decentri.dataapi.model.TokenFlowsDTO
import fi.decentri.dataapi.model.TransferSizeDistributionDTO
import fi.decentri.dataapi.model.TransferSizeRange
import fi.decentri.dataapi.model.TopCounterpartiesDTO
import fi.decentri.dataapi.model.Counterparty
import fi.decentri.dataapi.repository.TransferEventRepository
import fi.decentri.dataapi.service.EventService
import fi.decentri.dataapi.service.GasUsageService
import fi.decentri.dataapi.service.TokenFlowService
import fi.decentri.dataapi.service.TokenService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random
import kotlin.time.ExperimentalTime

private val logger = LoggerFactory.getLogger("fi.decentri.dataapi.routes.AnalyticsRoutes")

@ExperimentalTime
fun Route.analyticsRoutes(
    gasUsageService: GasUsageService,
    eventService: EventService,
    tokenService: TokenService,
) {
    route("/data") {
        // Gas usage endpoints
        get("/{network}/{contract}/gas-used/daily") {
            try {
                val network = call.parameters["network"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing network parameter"
                )
                val contract = call.parameters["contract"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing contract parameter"
                )

                logger.info("Fetching daily gas usage for network=$network, contract=$contract")
                val gasUsageData = gasUsageService.getInvocationData(network, contract)
                call.respond(gasUsageData)
            } catch (e: Exception) {
                logger.error("Error fetching gas usage data", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Unique addresses endpoints
        get("/{network}/{contract}/active-users/1d") {
            try {
                val network = call.parameters["network"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing network parameter"
                )
                val contract = call.parameters["contract"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing contract parameter"
                )

                logger.info("Fetching unique from_addresses count for network=$network, contract=$contract")
                val uniqueAddressesData = gasUsageService.getUniqueAddressesCount(
                    network, contract, LocalDateTime.now().minusDays(1)
                )
                call.respond(uniqueAddressesData)
            } catch (e: Exception) {
                logger.error("Error fetching unique addresses data", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Active users endpoints
        get("/{network}/{contract}/active-users/30min") {
            try {
                val network = call.parameters["network"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing network parameter"
                )
                val contract = call.parameters["contract"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing contract parameter"
                )

                logger.info("Fetching active users in last 30 minutes for network=$network, contract=$contract")
                // For now, we return a static value of 30

                val uniqueAddressesData = gasUsageService.getUniqueAddressesCount(
                    network, contract, LocalDateTime.now().minusMinutes(30)
                )

                call.respond(uniqueAddressesData)
            } catch (e: Exception) {
                logger.error("Error fetching active users data", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Events endpoints
        get("/{network}/{contract}/events/daily") {
            try {
                val filters = call.parseJsonbFilters()

                val network = call.parameters["network"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing network parameter"
                )
                val contract = call.parameters["contract"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing contract parameter"
                )

                logger.info("Fetching hourly event counts for network=$network, contract=$contract")
                val eventsData = eventService.getHourlyEventCounts(network, contract, filters)
                call.respond(eventsData)
            } catch (e: Exception) {
                logger.error("Error fetching events data", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Decoded event keys endpoints
        get("/{network}/{contract}/events/decoded-keys") {
            try {
                val network = call.parameters["network"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing network parameter"
                )
                val contract = call.parameters["contract"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing contract parameter"
                )

                logger.info("Fetching decoded event keys for network=$network, contract=$contract")
                val decodedKeys = eventService.getDecodedEventKeys(network, contract)
                call.respond(decodedKeys)
            } catch (e: Exception) {
                logger.error("Error fetching decoded event keys", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Token flows endpoint
        get("/{network}/{contract}/token-flows") {
            try {
                val network = call.parameters["network"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing network parameter"
                )
                val contract = call.parameters["contract"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing contract parameter"
                )
                
                val daysSince = call.request.queryParameters["daysSince"]?.toIntOrNull() ?: 365
                
                logger.info("Fetching token flows for network=$network, contract=$contract, daysSince=$daysSince")

                // Get from repository or fallback to sample data if TransferEventRepository throws
                val transferEventRepository = TransferEventRepository()
                val tokenFlowService = TokenFlowService(transferEventRepository, tokenService)
                val tokenFlowsData = tokenFlowService.getTokenFlows(network, contract, daysSince)
                call.respond(tokenFlowsData)
            } catch (e: Exception) {
                logger.error("Error fetching token flows data", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Transfer size distribution endpoint
        get("/{network}/{contract}/transfer-size-distribution") {
            try {
                val network = call.parameters["network"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing network parameter"
                )
                val contract = call.parameters["contract"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing contract parameter"
                )
                
                logger.info("Fetching transfer size distribution for network=$network, contract=$contract")
                
                // Stubbed data for now
                val distribution = listOf(
                    TransferSizeRange("0-10", Random.nextInt(100, 1000), 0.0),
                    TransferSizeRange("10-100", Random.nextInt(800, 3000), 0.0),
                    TransferSizeRange("100-1k", Random.nextInt(600, 2500), 0.0),
                    TransferSizeRange("1k-10k", Random.nextInt(200, 1200), 0.0),
                    TransferSizeRange("10k-100k", Random.nextInt(50, 500), 0.0),
                    TransferSizeRange("100k+", Random.nextInt(10, 100), 0.0)
                )
                
                val totalTransfers = distribution.sumOf { it.count }
                
                // Calculate percentages
                val distributionWithPercentages = distribution.map { range ->
                    range.copy(percentage = (range.count.toDouble() / totalTransfers) * 100)
                }
                
                val token = tokenService.getToken(network, contract)
                val response = TransferSizeDistributionDTO(
                    network = network,
                    contract = contract,
                    period = "last_30_days",
                    tokenSymbol = token?.name ?: "TOKEN",
                    totalTransfers = totalTransfers,
                    distribution = distributionWithPercentages
                )
                
                call.respond(response)
            } catch (e: Exception) {
                logger.error("Error fetching transfer size distribution", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
        
        // Top counterparties endpoint
        get("/{network}/{contract}/top-counterparties") {
            try {
                val network = call.parameters["network"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing network parameter"
                )
                val contract = call.parameters["contract"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing contract parameter"
                )
                
                logger.info("Fetching top counterparties for network=$network, contract=$contract")
                
                // Stubbed data for now - generate random addresses and volumes
                val counterparties = mutableListOf<Counterparty>()
                var totalVolume = 0.0
                
                for (i in 1..10) {
                    val address = "0x${Random.nextBytes(20).joinToString("") { "%02x".format(it) }}"
                    val shortAddress = "${address.take(6)}...${address.takeLast(4)}"
                    val volume = Random.nextDouble(10000.0, 1000000.0)
                    val transactionCount = Random.nextInt(50, 1000)
                    
                    totalVolume += volume
                    
                    counterparties.add(
                        Counterparty(
                            address = address,
                            shortAddress = shortAddress,
                            volume = volume,
                            transactionCount = transactionCount,
                            percentage = 0.0 // Will calculate after
                        )
                    )
                }
                
                // Sort by volume (descending) and calculate percentages
                val sortedCounterparties = counterparties
                    .sortedByDescending { it.volume }
                    .map { counterparty ->
                        counterparty.copy(percentage = (counterparty.volume / totalVolume) * 100)
                    }
                
                val token = tokenService.getToken(network, contract)
                val response = TopCounterpartiesDTO(
                    network = network,
                    contract = contract,
                    period = "last_30_days",
                    tokenSymbol = token?.name ?: "TOKEN",
                    tokenDecimals = token?.decimals ?: 18,
                    totalVolume = totalVolume,
                    counterparties = sortedCounterparties
                )
                
                call.respond(response)
            } catch (e: Exception) {
                logger.error("Error fetching top counterparties", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}