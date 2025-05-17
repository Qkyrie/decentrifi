package fi.decentri.dataapi.routes

import fi.decentri.dataapi.model.TransferSizeDistributionDTO
import fi.decentri.dataapi.model.TransferSizeRange
import fi.decentri.dataapi.model.TopCounterpartiesDTO
import fi.decentri.dataapi.model.Counterparty
import fi.decentri.dataapi.repository.TransferEventRepository
import fi.decentri.dataapi.service.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.time.ExperimentalTime

private val logger = LoggerFactory.getLogger("fi.decentri.dataapi.routes.AnalyticsRoutes")

@ExperimentalTime
fun Route.analyticsRoutes(
    gasUsageService: GasUsageService,
    counterPartyService: CounterPartyService,
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
                val safe = call.parameters["contract"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing contract parameter"
                )

                val daysSince = call.request.queryParameters["daysSince"]?.toIntOrNull() ?: 30

                logger.info("Fetching top counterparties for network=$network, contract=$safe, daysSince=$daysSince")

                // Fetch real data from the repository
                val counterpartiesData = counterPartyService.getTopCounterparties(
                    network = network,
                    safe = safe,
                    daysToLookBack = daysSince,
                    limit = 10
                )

                // Calculate total volume from all counterparties
                val totalVolumeBigInteger = counterpartiesData.sumOf { it.totalVolume }

                // Get token info for conversions
                val token = tokenService.getToken(network, safe)
                val tokenDecimals = token?.decimals ?: 18

                // Convert volumes to double and calculate percentages
                val counterparties = counterpartiesData.map { counterpartyData ->
                    val volumeAsDouble =
                        counterpartyData.totalVolume.toDouble() / Math.pow(10.0, tokenDecimals.toDouble())
                    val percentage = if (totalVolumeBigInteger > BigInteger.ZERO) {
                        (counterpartyData.totalVolume.toDouble() / totalVolumeBigInteger.toDouble()) * 100
                    } else {
                        0.0
                    }

                    Counterparty(
                        address = counterpartyData.address,
                        shortAddress = "${counterpartyData.address.take(6)}...${counterpartyData.address.takeLast(4)}",
                        volume = volumeAsDouble,
                        transactionCount = counterpartyData.transactionCount,
                        percentage = percentage
                    )
                }

                val totalVolumeDouble = totalVolumeBigInteger.toDouble() / Math.pow(10.0, tokenDecimals.toDouble())

                val periodDescription = when (daysSince) {
                    30 -> "last_30_days"
                    365 -> "last_year"
                    0 -> "all_time"
                    else -> "last_${daysSince}_days"
                }

                val response = TopCounterpartiesDTO(
                    network = network,
                    contract = safe,
                    period = periodDescription,
                    tokenSymbol = token?.name ?: "TOKEN",
                    tokenDecimals = tokenDecimals,
                    totalVolume = totalVolumeDouble,
                    counterparties = counterparties
                )

                call.respond(response)
            } catch (e: Exception) {
                logger.error("Error fetching top counterparties", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}