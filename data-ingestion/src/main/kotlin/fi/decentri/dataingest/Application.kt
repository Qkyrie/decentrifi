package fi.decentri.dataingest

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.typesafe.config.ConfigFactory
import fi.decentri.abi.AbiService
import fi.decentri.block.BlockService
import fi.decentri.dataingest.config.AppConfig
import fi.decentri.dataingest.config.Web3jManager
import fi.decentri.dataingest.ingest.EventIngestorService
import fi.decentri.dataingest.ingest.RawInvocationIngestorService
import fi.decentri.dataingest.model.Contract
import fi.decentri.dataingest.repository.ContractsRepository
import fi.decentri.dataingest.service.BlockchainIngestor
import fi.decentri.dataingest.service.ContractsService
import fi.decentri.db.DatabaseFactory
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime

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

        // Initialize Web3j manager
        val web3jManager = Web3jManager.init(ConfigFactory.load())

        // Create necessary services
        val contractsRepository = ContractsRepository()
        val abiService = AbiService()
        val contractsService = ContractsService(contractsRepository, abiService)
        val blockService = BlockService(web3jManager)

        // Use network parameter or default to ethereum
        val networkToUse = network ?: "ethereum"

        // Verify the network is configured
        if (!web3jManager.getNetworkNames().contains(networkToUse)) {
            logger.error("Network '$networkToUse' is not configured in application.conf")
            exitProcess(1)
        }

        // Get Web3j instance for the specified network
        val web3j = web3jManager.web3(networkToUse) ?: run {
            logger.error("Failed to create Web3j instance for network: $networkToUse")
            exitProcess(1)
        }

        val rawInvocationIngestorService = RawInvocationIngestorService(Web3jManager.getInstance())
        val eventIngestorService = EventIngestorService(Web3jManager.getInstance())

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
                    // In auto mode, the BlockchainIngestor will skip contracts that
                    // have been processed within the last 30 minutes to avoid duplicating
                    // work when manual ingestion jobs have been run
                    logger.info("Running in AUTO mode - processing all contracts (30-minute cooldown applied)")
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
            web3jManager.shutdown()
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