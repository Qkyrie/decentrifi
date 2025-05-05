package fi.decentri.dataingest

import com.fasterxml.jackson.databind.SerializationFeature
import fi.decentri.abi.AbiService
import fi.decentri.dataingest.config.AppConfig
import fi.decentri.db.DatabaseFactory
import fi.decentri.dataingest.ingest.IngestorService
import fi.decentri.dataingest.repository.ContractsRepository
import fi.decentri.dataingest.service.BlockchainIngestor
import fi.decentri.dataingest.service.ContractsService
import fi.decentri.db.rawinvocation.RawInvocations
import fi.decentri.waitlist.EmailRequest
import fi.decentri.waitlist.WaitlistRepository
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import kotlin.time.ExperimentalTime

val logger = LoggerFactory.getLogger("fi.decentri.dataingest.Application")

@ExperimentalTime
fun main() {
    logger.info("Starting data ingestion application")

    // Load configuration
    val appConfig = AppConfig.load()

    // Initialize database
    DatabaseFactory.init(appConfig.database)

    // Initialize database tables
    DatabaseFactory.initTables(
        RawInvocations,
        fi.decentri.dataingest.model.IngestionMetadata,
        fi.decentri.dataingest.model.Contracts,
        fi.decentri.waitlist.WaitlistEntries
    )

    // Start the server
    embeddedServer(Netty, port = appConfig.server.port) {
        configureRouting()
        configureSerialization()

        // Create necessary services
        val contractsRepository = ContractsRepository()
        val abiService = AbiService()
        val contractsService = ContractsService(contractsRepository, abiService)
        val web3j: Web3j = Web3j.build(HttpService(appConfig.ethereum.rpcUrl))

        val ingestorService = IngestorService(appConfig.ethereum, web3j)

        // Create and start the blockchain ingestion service
        val blockchainIngestor = BlockchainIngestor(
            contractsService,
            ingestorService,
            CoroutineScope(coroutineContext)
        )

        // Start the blockchain data ingestion service
        blockchainIngestor.startIngestion()
    }.start(wait = true)
}

fun Application.configureRouting() {
    // Create a waitlist repository
    val waitlistRepository = WaitlistRepository()
    
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
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
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}
