package fi.decentri.dataapi.routes

import fi.decentri.dataapi.repository.ContractsRepository
import fi.decentri.dataapi.repository.IngestionMetadataRepository
import fi.decentri.dataapi.service.*
import fi.decentri.dataapi.waitlist.WaitlistRepository
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun Application.configureRoutesModules(
    gasUsageService: GasUsageService,
    counterPartyService: CounterPartyService,
    eventService: EventService,
    waitlistRepository: WaitlistRepository,
    contractsRepository: ContractsRepository,
    ingestionMetadataRepository: IngestionMetadataRepository,
    contractsService: ContractsService,
    tokenService: TokenService
) {
    routing {
        // Base routes like health check, landing page
        baseRoutes()

        // Contract-related routes
        contractRoutes(contractsRepository, ingestionMetadataRepository, contractsService)

        // Waitlist routes
        waitlistRoutes(waitlistRepository)

        // Analytics routes
        analyticsRoutes(gasUsageService, counterPartyService, eventService, tokenService)
    }
}