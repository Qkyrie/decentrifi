    package fi.decentri.dataingest

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import fi.decentri.abi.AbiService
import fi.decentri.dataingest.config.AppConfig
import fi.decentri.dataingest.ingest.EventIngestorService
import fi.decentri.dataingest.ingest.RawInvocationIngestorService
import fi.decentri.dataingest.model.Contract
import fi.decentri.dataingest.repository.ContractsRepository
import fi.decentri.dataingest.service.BlockchainIngestor
import fi.decentri.dataingest.service.ContractsService
import fi.decentri.db.DatabaseFactory
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.util.concurrent.Executors
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration

    val logger = LoggerFactory.getLogger("fi.decentri.dataingest.Application")

/**
 * Command-line arguments parser for the data ingestion application.
 */
@ExperimentalTime
class IngestCommand : CliktCommand(
    name = "decentrifi-ingest",
    help = "Data ingestion application for blockchain contracts"
) {
    private val mode by option(
        "--mode",
        help = "Ingestion mode: 'auto' processes all contracts, 'contract' processes a specific contract"
    ).default("auto")

    private val contractAddress by option(
        "--contract",
        help = "Contract address to ingest data for (required in 'contract' mode)"
    )

    private val network by option(
        "--network",
        help = "Blockchain network for the contract (required in 'contract' mode)"
    )

    @OptIn(ExperimentalTime::class)
    override fun run() = runBlocking {
        logger.info("Starting data ingestion application in mode: $mode")

        // Load configuration
        val appConfig = AppConfig.load()

        // Initialize database
        DatabaseFactory.init(appConfig.database)

        // Create necessary services
        val contractsRepository = ContractsRepository()
        val abiService = AbiService()
        val contractsService = ContractsService(contractsRepository, abiService)

        // Web3j – supply your own scheduled executor so you can shut it down
        val scheduler = Executors.newScheduledThreadPool(4)
        val web3j = Web3j.build(
            HttpService(
                appConfig.ethereum.rpcUrl,
                OkHttpClient.Builder()
                    .connectTimeout(20.seconds.toJavaDuration())
                    .build()
            ), 2_000L, scheduler
        )

        // Create ingestor services
        val rawInvocationIngestorService = RawInvocationIngestorService(appConfig.ethereum, web3j)
        val eventIngestorService = EventIngestorService(appConfig.ethereum, web3j)

        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        // Create the blockchain ingestion service
        val blockchainIngestor = BlockchainIngestor(
            contractsService,
            rawInvocationIngestorService,
            eventIngestorService,
            applicationScope,
        )

        try {
            when (mode) {
                "auto" -> {
                    // Auto mode: Ingest data for all contracts
                    logger.info("Running in AUTO mode - processing all contracts")
                    val job = blockchainIngestor.startIngestion()
                    job.join() // Wait for the job to complete
                    logger.info("Ingestion job completed successfully")
                }
                "contract" -> {
                    // Contract mode: Ingest data for a specific contract
                    if (contractAddress == null || network == null) {
                        logger.error("Contract mode requires both --contract and --network parameters")
                        exitProcess(1)
                    }

                    logger.info("Running in CONTRACT mode - processing specific contract: $contractAddress on network: $network")
                    val contract = contractsService.findContractByAddressAndChain(contractAddress!!, network!!)

                    if (contract == null) {
                        logger.error("Contract not found: address=$contractAddress, network=$network")
                        exitProcess(1)
                    }

                    // Run ingestion for the specific contract
                    val job = ingestForSpecificContract(
                        contract,
                        applicationScope,
                        rawInvocationIngestorService,
                        eventIngestorService
                    )
                    job.join() // Wait for the job to complete
                    logger.info("Contract-specific ingestion job completed successfully")
                }
                else -> {
                    logger.error("Invalid mode: $mode. Valid options are 'auto' or 'contract'")
                    exitProcess(1)
                }
            }
        } catch (e: Exception) {
            logger.error("Error during ingestion job: ${e.message}", e)
        } finally {
            // Shutdown resources
            applicationScope.cancel()
            web3j.shutdown()
            scheduler.shutdown()
            logger.info("Application shutdown complete")
        }
    }

    /**
     * Run ingestion for a specific contract.
     */
    @OptIn(ExperimentalTime::class)
    private suspend fun ingestForSpecificContract(
        contract: Contract,
        scope: CoroutineScope,
        rawInvocationIngestorService: RawInvocationIngestorService,
        eventIngestorService: EventIngestorService
    ): Job = scope.launch {
        logger.info("Starting ingestion for contract: ${contract.address} (${contract.name ?: "unnamed"}) on chain: ${contract.chain}")

        // Launch raw invocations ingestion
        val rawInvocationsJob = launch {
            try {
                rawInvocationIngestorService.ingest(contract)
                logger.info("Raw invocations ingestion complete for contract ${contract.address}: caught up with the latest block")
            } catch (e: Exception) {
                logger.error(
                    "Error during raw invocations ingestion for contract ${contract.address} (ID: ${contract.id}): ${e.message}",
                    e
                )
            }
        }

        // Launch events ingestion
        val eventsJob = launch {
            try {
                eventIngestorService.ingest(contract)
                logger.info("Events ingestion complete for contract ${contract.address}: caught up with the latest block")
            } catch (e: Exception) {
                logger.error(
                    "Error during events ingestion for contract ${contract.address} (ID: ${contract.id}): ${e.message}",
                    e
                )
            }
        }

        // Wait for both jobs to complete
        rawInvocationsJob.join()
        eventsJob.join()
    }
}

@ExperimentalTime
fun main(args: Array<String>) {
    IngestCommand().main(args)
}