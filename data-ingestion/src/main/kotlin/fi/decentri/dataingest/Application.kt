package fi.decentri.dataingest

import com.fasterxml.jackson.databind.SerializationFeature
import fi.decentri.dataingest.config.AppConfig
import fi.decentri.dataingest.db.DatabaseFactory
import fi.decentri.dataingest.ingest.IngestorService
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("fi.decentri.dataingest.Application")
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
            ingestorService.startIngestingDataWithTraceFilter()
        }
    }.start(wait = true)
}

fun Application.configureRouting() {
    routing {
        // API routes can be added here if needed
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
