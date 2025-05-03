package fi.decentri.dataapi

import com.fasterxml.jackson.databind.SerializationFeature
import fi.decentri.dataapi.config.AppConfig
import fi.decentri.dataapi.db.DatabaseFactory
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("fi.decentri.dataapi.Application")

fun main() {
    logger.info("Starting data API application")

    // Load configuration
    val appConfig = AppConfig.load()

    // Initialize database
    DatabaseFactory.init(appConfig.database)

    // Start the server
    embeddedServer(Netty, port = appConfig.server.port) {
        configureRouting()
        configureSerialization()
    }.start(wait = true)
}

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }
        
        get("/api/v1/contracts") {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Contracts endpoint - To be implemented"))
        }
        
        get("/api/v1/transactions") {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Transactions endpoint - To be implemented"))
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