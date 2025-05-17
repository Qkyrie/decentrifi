package fi.decentri.dataapi

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.decentri.dataapi.config.AppConfig
import fi.decentri.dataapi.repository.*
import fi.decentri.dataapi.routes.configureRoutesModules
import fi.decentri.dataapi.service.*
import fi.decentri.dataapi.waitlist.WaitlistRepository
import fi.decentri.db.DatabaseFactory
import fi.decentri.db.contract.Contracts
import fi.decentri.db.event.RawLogs
import fi.decentri.db.ingestion.IngestionMetadata
import fi.decentri.db.rawinvocation.RawInvocations
import fi.decentri.db.token.TransferEvents
import fi.decentri.db.waitlist.WaitlistEntries
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.thymeleaf.*
import org.slf4j.LoggerFactory
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import kotlin.time.ExperimentalTime


@ExperimentalTime
fun main() {
    val log = LoggerFactory.getLogger("fi.decentri.dataapi.Application")
    log.info("Starting data API application")

    // Load configuration
    val appConfig = AppConfig.load()

    // Initialize database
    DatabaseFactory.init(appConfig.database)

    DatabaseFactory.initTables(
        RawInvocations,
        RawLogs,
        IngestionMetadata,
        Contracts,
        WaitlistEntries,
        TransferEvents
    )

    // Initialize repositories
    val rawInvocationsRepository = RawInvocationsRepository()
    val waitlistRepository = WaitlistRepository()
    val rawLogsRepository = RawLogsRepository()
    val contractsRepository = ContractsRepository()
    val ingestionMetadataRepository = IngestionMetadataRepository()
    val contractsService = ContractsService(contractsRepository)
    val transferEventRepository = TransferEventRepository()

    // Initialize services
    val gasUsageService = GasUsageService(rawInvocationsRepository)
    val counterPartyService = CounterPartyService(transferEventRepository)
    val eventService = EventService(rawLogsRepository)
    val tokenService = TokenService()

    // Start the server
    embeddedServer(Netty, port = appConfig.server.port) {
        configureRoutesModules(
            gasUsageService,
            counterPartyService,
            eventService,
            waitlistRepository,
            contractsRepository,
            ingestionMetadataRepository,
            contractsService,
            tokenService
        )
        configureSerialization()
        configureTemplating()
    }.start(wait = true)
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
            findAndRegisterModules()
            registerModules(JavaTimeModule())
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}