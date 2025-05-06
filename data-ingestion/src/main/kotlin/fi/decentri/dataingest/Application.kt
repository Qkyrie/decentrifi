package fi.decentri.dataingest

import com.fasterxml.jackson.databind.SerializationFeature
import fi.decentri.abi.AbiService
import fi.decentri.dataingest.config.AppConfig
import fi.decentri.db.DatabaseFactory
import fi.decentri.dataingest.ingest.EventIngestorService
import fi.decentri.dataingest.ingest.RawInvocationIngestorService
import fi.decentri.dataingest.repository.ContractsRepository
import fi.decentri.dataingest.service.BlockchainIngestor
import fi.decentri.dataingest.service.ContractsService
import fi.decentri.db.rawinvocation.RawInvocations
import fi.decentri.db.event.RawLogs
import fi.decentri.db.event.EventDefinitions
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration

val logger = LoggerFactory.getLogger("fi.decentri.dataingest.Application")

@ExperimentalTime
suspend fun main() {
    logger.info("Starting data ingestion application")

    // Load configuration
    val appConfig = AppConfig.load()

    // Initialize database
    DatabaseFactory.init(appConfig.database)

    // Initialize database tables
    DatabaseFactory.initTables(
        RawInvocations,
        RawLogs,
        fi.decentri.dataingest.model.IngestionMetadata,
        fi.decentri.dataingest.model.Contracts,
        fi.decentri.db.waitlist.WaitlistEntries
    )


    // Create necessary services
    val contractsRepository = ContractsRepository()
    val abiService = AbiService()
    val contractsService = ContractsService(contractsRepository, abiService)
    val web3j: Web3j = Web3j.build(
        HttpService(
            appConfig.ethereum.rpcUrl, OkHttpClient.Builder()
                .connectTimeout(20.seconds.toJavaDuration())
                .build()
        )
    )

    // Create ingestor services
    val rawInvocationIngestorService = RawInvocationIngestorService(appConfig.ethereum, web3j)
    val eventIngestorService = EventIngestorService(appConfig.ethereum, web3j)

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Create and start the blockchain ingestion service
    val blockchainIngestor = BlockchainIngestor(
        contractsService,
        rawInvocationIngestorService,
        eventIngestorService,
        applicationScope,
    )

    try {
        // Start the ingestion job and wait for it to complete
        val job = blockchainIngestor.startIngestion()
        job.join() // Wait for the job to complete
        logger.info("Ingestion job completed successfully")
    } catch (e: Exception) {
        logger.error("Error during ingestion job: ${e.message}", e)
    } finally {
        // Shutdown resources
        applicationScope.cancel()
        web3j.shutdown()
        logger.info("Application shutdown complete")
    }
}