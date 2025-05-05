package fi.decentri.dataapi

import com.fasterxml.jackson.databind.SerializationFeature
import fi.decentri.dataapi.config.AppConfig
import fi.decentri.dataapi.repository.RawInvocationsRepository
import fi.decentri.dataapi.repository.RawLogsRepository
import fi.decentri.dataapi.service.EventService
import fi.decentri.dataapi.service.GasUsageService
import fi.decentri.db.DatabaseFactory
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import org.slf4j.LoggerFactory
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.io.File

val logger = LoggerFactory.getLogger("fi.decentri.dataapi.Application")

fun main() {
    logger.info("Starting data API application")

    // Load configuration
    val appConfig = AppConfig.load()

    // Initialize database
    DatabaseFactory.init(appConfig.database)

    // Initialize repositories
    val rawInvocationsRepository = RawInvocationsRepository()
    val rawLogsRepository = RawLogsRepository()
    
    // Initialize services
    val gasUsageService = GasUsageService(rawInvocationsRepository)
    val eventService = EventService(rawLogsRepository)

    // Start the server
    embeddedServer(Netty, port = appConfig.server.port) {
        configureRouting(gasUsageService, eventService)
        configureSerialization()
        configureTemplating()
    }.start(wait = true)
}

fun Application.configureRouting(gasUsageService: GasUsageService, eventService: EventService) {
    routing {

        staticResources("/images", "static/images")

        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }

        get("/") {
            call.respond(ThymeleafContent("analytics-landing.html", mapOf("title" to "Data Ingestion Service")))
        }

        get("/{network}/{contract}") {
            val network = call.parameters["network"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing network parameter")
            val contract = call.parameters["contract"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing contract parameter")
            call.respond(ThymeleafContent("contract-analytics.html", mapOf("title" to "Data Ingestion Service")))
        }
        
        // API endpoints
        route("/data") {
            get("/{network}/{contract}/gas-used/daily") {
                try {
                    val network = call.parameters["network"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing network parameter")
                    val contract = call.parameters["contract"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing contract parameter")
                    
                    logger.info("Fetching daily gas usage for network=$network, contract=$contract")
                    val gasUsageData = gasUsageService.getDailyGasUsage(network, contract)
                    call.respond(gasUsageData)
                } catch (e: Exception) {
                    logger.error("Error fetching gas usage data", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            
            get("/{network}/{contract}/events/daily") {
                try {
                    val network = call.parameters["network"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing network parameter")
                    val contract = call.parameters["contract"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing contract parameter")
                    
                    logger.info("Fetching events from last 24 hours for network=$network, contract=$contract")
                    val eventsData = eventService.getEventsFromLast24Hours(network, contract)
                    call.respond(eventsData)
                } catch (e: Exception) {
                    logger.error("Error fetching events data", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
        }
    }
}

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