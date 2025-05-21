package fi.decentri.dataapi.routes

import fi.decentri.dataapi.k8s.ingestion.IngestionLauncher
import fi.decentri.dataapi.model.ContractSubmission
import fi.decentri.dataapi.repository.ContractsRepository
import fi.decentri.dataapi.repository.IngestionJobRepository
import fi.decentri.dataapi.service.ContractsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

private val logger = LoggerFactory.getLogger("fi.decentri.dataapi.routes.ContractRoutes")

@ExperimentalTime
fun Route.contractRoutes(
    contractsRepository: ContractsRepository,
    ingestionJobRepository: IngestionJobRepository,
    contractsService: ContractsService
) {
    // Contract submission endpoint
    post("/contract/submit") {
        try {
            val contractSubmission = call.receive<ContractSubmission>()
            logger.info("Received contract submission for address: ${contractSubmission.contractAddress} on network: ${contractSubmission.network}")

            // Save contract to database
            val id = contractsRepository.insert(
                address = contractSubmission.contractAddress.lowercase(),
                abi = contractSubmission.abi,
                network = contractSubmission.network.lowercase(),
                type = contractSubmission.type
            )
            logger.info("Saved contract to database with ID: $id")

            try {
                val ingestionLauncher = IngestionLauncher()
                val jobName = ingestionLauncher.launchManualRun(
                    contractsService.getContract(id)!!
                )
                logger.info("Launched ingestion job: $jobName for contract: ${contractSubmission.contractAddress}")
            } catch (e: Exception) {
                logger.error(
                    "Failed to launch ingestion job for contract: ${contractSubmission.contractAddress}",
                    e
                )
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
            val contract =
                contractsRepository.findByAddressAndNetwork(contractAddress.lowercase(), network.lowercase())
            if (contract == null) {
                logger.warn("Contract not found for ingestion: $contractAddress on network: $network")
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Contract not found in database"))
                return@post
            }

            // Launch the ingestion job for this contract
            val ingestionLauncher = IngestionLauncher()
            val jobName = ingestionLauncher.launchManualRun(contract)
            logger.info("Manually launched ingestion job: $jobName for contract: $contractAddress on network: $network")

            call.respond(
                HttpStatusCode.OK,
                mapOf("job" to jobName, "message" to "Ingestion job launched successfully")
            )
        } catch (e: Exception) {
            logger.error("Failed to launch manual ingestion job", e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
        }
    }

    // Contract detail page route
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
        val hasMetadata = ingestionJobRepository.hasAnyCompletedJob(contract.id!!)
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

        when (contract.type.lowercase()) {
            "safe" -> {
                call.respond(
                    ThymeleafContent(
                        "safe-analytics.html", mapOf(
                            "title" to "Safe Analytics",
                            "network" to network,
                            "contract" to contractAddress
                        )
                    )
                )
            }
            else -> {
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
        }
    }

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
            val hasMetadata = ingestionJobRepository.hasAnyCompletedJob(contract.id!!)
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
}