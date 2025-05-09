package fi.decentri.dataapi

import com.fasterxml.jackson.databind.SerializationFeature
import fi.decentri.dataapi.config.AppConfig
import fi.decentri.dataapi.k8s.ingestion.IngestionLauncher
import fi.decentri.dataapi.model.ContractSubmission
import fi.decentri.dataapi.model.MetadataType
import fi.decentri.dataapi.repository.ContractsRepository
import fi.decentri.dataapi.repository.IngestionMetadataRepository
import fi.decentri.dataapi.repository.RawInvocationsRepository
import fi.decentri.dataapi.repository.RawLogsRepository
import fi.decentri.dataapi.service.EventService
import fi.decentri.dataapi.service.GasUsageService
import fi.decentri.dataapi.waitlist.EmailRequest
import fi.decentri.dataapi.waitlist.WaitlistRepository
import fi.decentri.db.DatabaseFactory
import fi.decentri.db.contract.Contracts
import fi.decentri.db.event.RawLogs
import fi.decentri.db.ingestion.IngestionMetadata
import fi.decentri.db.rawinvocation.RawInvocations
import fi.decentri.db.waitlist.WaitlistEntries
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import org.slf4j.LoggerFactory
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import kotlin.time.ExperimentalTime

val logger = LoggerFactory.getLogger("fi.decentri.dataapi.Application")

@ExperimentalTime
fun main() {
    logger.info("Starting data API application")

    // Load configuration
    val appConfig = AppConfig.load()

    // Initialize database
    DatabaseFactory.init(appConfig.database)

    DatabaseFactory.initTables(
        RawInvocations,
        RawLogs,
        IngestionMetadata,
        Contracts,
        WaitlistEntries
    )

    // Initialize repositories
    val rawInvocationsRepository = RawInvocationsRepository()
    val waitlistRepository = WaitlistRepository()
    val rawLogsRepository = RawLogsRepository()
    val contractsRepository = ContractsRepository()
    val ingestionMetadataRepository = IngestionMetadataRepository()

    // Initialize services
    val gasUsageService = GasUsageService(rawInvocationsRepository)
    val eventService = EventService(rawLogsRepository)

    // Start the server
    embeddedServer(Netty, port = appConfig.server.port) {
        configureRouting(
            gasUsageService,
            eventService,
            waitlistRepository,
            contractsRepository,
            ingestionMetadataRepository
        )
        configureSerialization()
        configureTemplating()
    }.start(wait = true)
}

@ExperimentalTime
fun Application.configureRouting(
    gasUsageService: GasUsageService,
    eventService: EventService,
    waitlistRepository: WaitlistRepository,
    contractsRepository: ContractsRepository,
    ingestionMetadataRepository: IngestionMetadataRepository
) {
    routing {

        staticResources("/images", "static/images")

        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }

        get("/") {
            call.respond(ThymeleafContent("analytics-landing.html", mapOf("title" to "Data Ingestion Service")))
        }

        post("/contract/submit") {
            try {
                val contractSubmission = call.receive<ContractSubmission>()
                logger.info("Received contract submission for address: ${contractSubmission.contractAddress} on network: ${contractSubmission.network}")

                // Save contract to database
                val id = contractsRepository.insert(
                    address = contractSubmission.contractAddress.lowercase(),
                    abi = contractSubmission.abi,
                    network = contractSubmission.network.lowercase()
                )
                logger.info("Saved contract to database with ID: $id")

                // Launch the ingestion job for this contract
                try {
                    val ingestionLauncher = IngestionLauncher()
                    val jobName = ingestionLauncher.launchManualRun(
                        contractSubmission.contractAddress.lowercase(),
                        contractSubmission.network.lowercase()
                    )
                    logger.info("Launched ingestion job: $jobName for contract: ${contractSubmission.contractAddress}")
                } catch (e: Exception) {
                    logger.error("Failed to launch ingestion job for contract: ${contractSubmission.contractAddress}", e)
                    // Continue execution, as this is not a critical error for the contract submission
                }

                call.respond(
                    HttpStatusCode.OK,
                    mapOf("location" to "/${contractSubmission.network}/${contractSubmission.contractAddress}")
                )
            } catch (e: Exception) {
                logger.error("Failed to process contract submission", e)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid contract submission"))
            }
        }

        // Manually trigger ingestion for a contract
        post("/{network}/{contract}/ingest") {
            try {
                val network = call.parameters["network"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing network parameter")
                )
                val contractAddress = call.parameters["contract"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing contract parameter")
                )

                // Check if the contract/network combination exists in our database
                val contract = contractsRepository.findByAddressAndNetwork(contractAddress.lowercase(), network.lowercase())
                if (contract == null) {
                    logger.warn("Contract not found for ingestion: $contractAddress on network: $network")
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Contract not found in database"))
                    return@post
                }

                // Launch the ingestion job for this contract
                val ingestionLauncher = IngestionLauncher()
                val jobName = ingestionLauncher.launchManualRun(contractAddress.lowercase(), network.lowercase())
                logger.info("Manually launched ingestion job: $jobName for contract: $contractAddress on network: $network")

                call.respond(HttpStatusCode.OK, mapOf("job" to jobName, "message" to "Ingestion job launched successfully"))
            } catch (e: Exception) {
                logger.error("Failed to launch manual ingestion job", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/waitlist") {
            try {
                val emailRequest = call.receive<EmailRequest>() // Receive the JSON payload
                logger.info("Received email for waitlist: ${emailRequest.email}")

                // Save email to database
                val id = waitlistRepository.insert(emailRequest.email)
                logger.info("Saved email to waitlist with ID: $id")

                call.respond(HttpStatusCode.OK, mapOf("message" to "Email received")) // Send success response
            } catch (e: Exception) {
                logger.error("Failed to process waitlist request", e)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request"))
            }
        }

        get("/{network}/{contract}") {
            val network = call.parameters["network"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Missing network parameter"
            )
            val contractAddress = call.parameters["contract"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Missing contract parameter"
            )

            // Check if the contract/network combination exists in our database
            val contract = contractsRepository.findByAddressAndNetwork(contractAddress.lowercase(), network.lowercase())
            if (contract == null) {
                logger.warn("Contract not found: $contractAddress on network: $network, redirecting to landing page")
                call.respondRedirect("/")
                return@get
            }

            // Check if ingestion metadata exists for this contract
            val hasMetadata = ingestionMetadataRepository.hasAnyMetadataForContract(contract.id!!)
            if (!hasMetadata) {
                logger.info("No ingestion metadata found for contract: $contractAddress on network: $network, showing processing page")
                call.respond(
                    ThymeleafContent(
                        "contract-processing.html", mapOf(
                            "title" to "Processing Smart Contract",
                            "network" to network,
                            "contract" to contractAddress
                        )
                    )
                )
                return@get
            }

            // Contract exists and has metadata, show the analytics page
            call.respond(
                ThymeleafContent(
                    "contract-analytics.html", mapOf(
                        "title" to "Contract Analytics",
                        "network" to network,
                        "contract" to contractAddress
                    )
                )
            )
        }

        // API endpoints
        // Status endpoint to check if contract has metadata
        get("/{network}/{contract}/status") {
            try {
                val network = call.parameters["network"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "unknown", "message" to "Missing network parameter")
                )
                val contractAddress = call.parameters["contract"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "unknown", "message" to "Missing contract parameter")
                )

                // Check if the contract/network combination exists in our database
                val contract =
                    contractsRepository.findByAddressAndNetwork(contractAddress.lowercase(), network.lowercase())
                if (contract == null) {
                    logger.warn("Contract not found: $contractAddress on network: $network")
                    call.respond(mapOf("status" to "unknown"))
                    return@get
                }

                // Check if ingestion metadata exists for this contract
                val hasMetadata = ingestionMetadataRepository.hasAnyMetadataForContract(contract.id!!)
                if (!hasMetadata) {
                    logger.info("No ingestion metadata found for contract: $contractAddress on network: $network")
                    call.respond(mapOf("status" to "processing"))
                    return@get
                }

                // Contract exists and has metadata
                call.respond(mapOf("status" to "done"))
            } catch (e: Exception) {
                logger.error("Error checking contract status", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("status" to "unknown", "error" to e.message))
            }
        }

        route("/data") {
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

            get("/{network}/{contract}/unique-addresses") {
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
                    val uniqueAddressesData = gasUsageService.getUniqueAddressesCount(network, contract)
                    call.respond(uniqueAddressesData)
                } catch (e: Exception) {
                    logger.error("Error fetching unique addresses data", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }

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
        }
    }
}

data class JsonbFilter(val key: String, val value: String)

fun ApplicationCall.parseJsonbFilters(): List<JsonbFilter> =
    request.queryParameters.getAll("filter")
        ?.mapNotNull { token ->
            token.split(":", limit = 2)
                .takeIf { it.size == 2 }
                ?.let { (k, v) -> JsonbFilter(k, v) }
        }
        ?: emptyList()

fun Application.configureTemplating() {
    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            // Register JavaTimeModule to handle Java 8 date/time types
            findAndRegisterModules()
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}