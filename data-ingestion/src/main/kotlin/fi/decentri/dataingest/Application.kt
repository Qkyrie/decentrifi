package fi.decentri.dataingest

import com.fasterxml.jackson.databind.SerializationFeature
import fi.decentri.dataingest.config.AppConfig
import fi.decentri.dataingest.db.DatabaseFactory
import fi.decentri.dataingest.ingest.IngestorService
import fi.decentri.waitlist.EmailRequest
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("fi.decentri.dataingest.Application")

fun main() {
    logger.info("Starting data ingestion application")

    // Load configuration
    val appConfig = AppConfig.load()

    // Initialize database
    DatabaseFactory.init(appConfig.database)

    // Start the server
    embeddedServer(Netty, port = appConfig.server.port) {
        configureRouting()
        configureSerialization()

        // Start the blockchain data ingestion process in a background coroutine
        val ingestorService = IngestorService(appConfig.ethereum)
        launch(Dispatchers.IO) {
            logger.info("Starting blockchain data ingestion service with trace_filter")
            ingestorService.ingest()
            logger.info("caught up with the latest block")
        }
    }.start(wait = true)
}

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }
        
        post("/waitlist") {
            try {
                val emailRequest = call.receive<EmailRequest>() // Receive the JSON payload
                logger.info("Received email for waitlist: ${emailRequest.email}") // Log the email
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
