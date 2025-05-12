package fi.decentri.dataapi.routes

import fi.decentri.dataapi.repository.ContractsRepository
import fi.decentri.dataapi.repository.IngestionMetadataRepository
import fi.decentri.dataapi.service.EventService
import fi.decentri.dataapi.service.GasUsageService
import fi.decentri.dataapi.waitlist.WaitlistRepository
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun Application.configureRoutesModules(
    gasUsageService: GasUsageService,
    eventService: EventService,
    waitlistRepository: WaitlistRepository,
    contractsRepository: ContractsRepository,
    ingestionMetadataRepository: IngestionMetadataRepository
) {
    routing {
        // Base routes like health check, landing page
        baseRoutes()

        // Contract-related routes
        contractRoutes(contractsRepository, ingestionMetadataRepository)

        // Waitlist routes
        waitlistRoutes(waitlistRepository)

        // Analytics routes
        analyticsRoutes(gasUsageService, eventService)
    }
}