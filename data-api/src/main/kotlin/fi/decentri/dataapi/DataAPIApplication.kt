package fi.decentri.dataapi

import com.fasterxml.jackson.databind.SerializationFeature
import fi.decentri.dataapi.config.AppConfig
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

    // Start the server
    embeddedServer(Netty, port = appConfig.server.port) {
        configureRouting()
        configureSerialization()
        configureTemplating()
    }.start(wait = true)
}

fun Application.configureRouting() {
    routing {

        staticResources("/images", "static/images")

        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }

        get("/") {
            call.respond(ThymeleafContent("contract-analytics.html", mapOf("title" to "Data Ingestion Service")))
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
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}