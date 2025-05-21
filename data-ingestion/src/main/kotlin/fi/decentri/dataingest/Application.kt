package fi.decentri.dataingest

import com.github.ajalt.clikt.core.CliktCommand
import com.typesafe.config.ConfigFactory
import fi.decentri.application.usecases.EventIngestor
import fi.decentri.application.usecases.RawInvocationIngestor
import fi.decentri.application.usecases.TokenTransferIngestor
import fi.decentri.block.BlockService
import fi.decentri.dataingest.config.AppConfig
import fi.decentri.dataingest.config.Web3jManager
import fi.decentri.dataingest.service.ContractsService
import fi.decentri.dataingest.service.JobProcessorService
import fi.decentri.dataingest.service.JobSchedulerService
import fi.decentri.dataingest.service.JobService
import fi.decentri.db.DatabaseFactory
import fi.decentri.db.ingestion.Jobs
import fi.decentri.infrastructure.abi.AbiService
import fi.decentri.infrastructure.repository.contract.ContractsRepository
import fi.decentri.infrastructure.repository.ingestion.EventRepository
import fi.decentri.infrastructure.repository.ingestion.IngestionMetadataRepository
import fi.decentri.infrastructure.repository.ingestion.JobRepository
import fi.decentri.infrastructure.repository.ingestion.RawInvocationRepository
import fi.decentri.infrastructure.repository.token.TransferEventRepository
import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime


/**
 * Command-line arguments parser for the data ingestion application.
 */
@ExperimentalTime
class IngestCommand : CliktCommand(
    name = "decentrifi-ingest",
    help = "Data ingestion application for blockchain contracts"
) {

    companion object {
        val logger = LoggerFactory.getLogger(Application::class.java)
    }

    override fun run() {
        runBlocking {
            logger.info("Starting data ingestion application in mode")

            // Load configuration
            val appConfig = AppConfig.load()

            // Initialize database
            DatabaseFactory.init(appConfig.database)

            // Initialize required database tables
            DatabaseFactory.initTables(Jobs) // Make sure the Jobs table is created

            // Initialize Web3j manager
            val web3jManager = Web3jManager.init(ConfigFactory.load())

            // Create adapters
            val contractsRepository = ContractsRepository()
            val abiPort = AbiService()
            val contractsService = ContractsService(contractsRepository, abiPort)
            val metadataRepository = IngestionMetadataRepository()
            val rawInvocationRepository = RawInvocationRepository()
            val eventRepository = EventRepository()
            val transferEventRepository = TransferEventRepository()
            val blockService = BlockService(Web3jManager.getInstance())
            val jobRepository = JobRepository()
            val abiService = AbiService()

            // Create use cases
            val rawInvocationIngestor = RawInvocationIngestor(
                Web3jManager.getInstance(),
                metadataRepository,
                rawInvocationRepository,
                blockService
            )

            val eventIngestor = EventIngestor(
                Web3jManager.getInstance(),
                metadataRepository,
                eventRepository,
                blockService,
                abiPort
            )

            val tokenTransferIngestor = TokenTransferIngestor(
                Web3jManager.getInstance(),
                metadataRepository,
                transferEventRepository,
                blockService
            )

            val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

            // Create job services
            val jobService = JobService(jobRepository, applicationScope)

            val jobProcessorService = JobProcessorService(
                jobService,
                contractsService,
                rawInvocationIngestor,
                eventIngestor,
                tokenTransferIngestor,
            )

            val jobSchedulerService = JobSchedulerService(
                jobService,
                contractsService,
                abiService,
                blockService,
                metadataRepository,
            )

            // Register shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(Thread {
                logger.info("Shutdown hook triggered, cancelling jobs...")
                runBlocking {
                    applicationScope.cancel()
                    web3jManager.shutdown()
                }
                logger.info("Shutdown hook completed")
            })

            try {
                // Job mode: Use the job scheduler and processor to manage ingestion tasks
                logger.info("Running in JOB mode - using job-based processing system")

                // Start the job scheduler service
                val scheduler = jobSchedulerService.run()

                // Start the job processor service
                val processor = jobProcessorService.run()

                listOf(scheduler, processor).joinAll()
                logger.info("Job services clompleted successfully. Application will die now.")
            } catch (e: Exception) {
                logger.error("Error during ingestion job: ${e.message}", e)
            } finally {
                // Only clean up if we're not in job mode (which should run continuously)
                logger.info("Application cleanup initiated")
                // Cancel all coroutines
                applicationScope.cancel()

                // Give coroutines time to finish
                delay(1000)

                // Shutdown Web3j connections
                web3jManager.shutdown()

                logger.info("Application shutdown complete, exiting...")

                exitProcess(0)
            }
        }
    }
}

@ExperimentalTime
fun main(args: Array<String>) {
    IngestCommand().main(args)
}