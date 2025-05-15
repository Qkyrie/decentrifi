package fi.decentri.dataapi.routes

import fi.decentri.dataapi.model.TokenFlowPoint
import fi.decentri.dataapi.model.TokenFlowsDTO
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
                
                // Parse the daysSince parameter with a default of 365 days
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
        
        // Maintain backward compatibility with the old endpoint
        get("/{network}/{contract}/token-flows/monthly") {
            try {
                val network = call.parameters["network"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing network parameter"
                )
                val contract = call.parameters["contract"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing contract parameter"
                )

                logger.info("Fetching monthly token flows for network=$network, contract=$contract")

                // Get from repository or fallback to sample data if TransferEventRepository throws
                val transferEventRepository = TransferEventRepository()
                val tokenFlowService = TokenFlowService(transferEventRepository, tokenService)
                // Use 365 days for monthly view
                val tokenFlowsData = tokenFlowService.getTokenFlows(network, contract, 365)
                call.respond(tokenFlowsData)
            } catch (e: Exception) {
                logger.error("Error fetching token flows data", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}