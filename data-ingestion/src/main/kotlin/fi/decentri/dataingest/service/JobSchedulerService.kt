package fi.decentri.dataingest.service

import fi.decentri.application.ports.BlockPort
import fi.decentri.application.ports.IngestionMetadataPort
import fi.decentri.dataingest.model.Contract
import fi.decentri.dataingest.model.MetadataType
import fi.decentri.db.ingestion.JobType
import fi.decentri.infrastructure.abi.AbiService
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import kotlin.time.ExperimentalTime

/**
 * Service responsible for scheduling ingestion jobs
 */
@OptIn(ExperimentalTime::class)
class JobSchedulerService(
    private val jobService: JobService,
    private val contractsService: ContractsService,
    private val abiService: AbiService,
    private val blockService: BlockPort,
    private val metadataRepository: IngestionMetadataPort,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Start the job scheduler
     */
    suspend fun run() {
        logger.info("Starting job scheduler service")
        scheduleJobs()
    }

    /**
     * Main job scheduling loop
     */
    private suspend fun scheduleJobs() {
        try {
            logger.debug("Checking for contracts that need jobs scheduled")
            val contracts = contractsService.getAllContracts()

            if (contracts.isEmpty()) {
                logger.warn("No contracts found in the database. No jobs will be scheduled.")
            } else {
                logger.info("Found ${contracts.size} contracts to process")
                processContracts(contracts)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.error("Error in job scheduler: ${e.message}", e)
        }
    }

    /**
     * Process all contracts and schedule jobs as needed
     */
    private suspend fun processContracts(contracts: List<Contract>) {
        for (contract in contracts) {
            try {
                if (jobService.hasRunningOrPendingJobsForContract(contract.id!!)) {
                    logger.debug("Skipping contract ${contract.address} as it has running jobs")
                    continue
                }

                // Schedule jobs for the contract
                scheduleJobsForContract(contract)
            } catch (e: Exception) {
                logger.error("Error processing contract ${contract.address}: ${e.message}", e)
            }
        }
    }

    /**
     * Schedule necessary jobs for a contract
     */
    private suspend fun scheduleJobsForContract(contract: Contract) {
        scheduleRawInvocationsRunupJob(contract)
        scheduleEventsRunupJob(contract)

        // Schedule token transfer jobs only for Safe contracts
        if (contract.type == "safe") {
            scheduleTokenTransferRunupJob(contract)
        }
    }

    /**
     * Schedule a raw invocations job if needed
     */
    private suspend fun scheduleRawInvocationsRunupJob(contract: Contract) {
        val latestJob = jobService.getLatestCompletedJobByTypeAndContract(JobType.RAW_INVOCATIONS, contract.id!!)

        // Get the last processed block from metadata or from the latest job
        val lastProcessedBlock = latestJob?.metadata?.get("endBlock")?.toString()?.toLongOrNull()
            ?: metadataRepository.getMetadatForContractId(
                MetadataType.LAST_PROCESSED_BLOCK_RAW_INVOCATIONS,
                contract.id
            )
                ?.toLongOrNull()
            ?: blockService.getBlockClosestTo(LocalDateTime.now().minusHours(24), contract.chain)

        // Get the latest block on the chain
        val latestBlock = blockService.getLatestBlock(contract.chain)

        // If we're already caught up, don't schedule a job
        if (lastProcessedBlock >= latestBlock) {
            logger.debug("Contract ${contract.address} raw invocations are up to date (block $lastProcessedBlock)")
            return
        }

        logger.info("Scheduling raw invocations job for contract ${contract.address} from block ${lastProcessedBlock + 1} to $latestBlock")

        // Create the job
        val metadata = mapOf(
            "startBlock" to (lastProcessedBlock + 1),
            "endBlock" to latestBlock
        )

        jobService.createJob(JobType.RAW_INVOCATIONS, contract.id, metadata)
    }

    /**
     * Schedule an events job if needed
     */
    private suspend fun scheduleEventsRunupJob(contract: Contract) {
        // Parse ABI to check if the contract has events
        val (_, events) = abiService.parseABI(contract.abi)
        if (events.isEmpty()) {
            logger.debug("Contract ${contract.address} has no events defined in its ABI. Skipping events job.")
            return
        }

        val latestJob = jobService.getLatestCompletedJobByTypeAndContract(JobType.EVENTS, contract.id!!)

        // Get the last processed block from metadata or from the latest job
        val lastProcessedBlock = latestJob?.metadata?.get("endBlock")?.toString()?.toLongOrNull()
            ?: metadataRepository.getMetadatForContractId(MetadataType.LAST_PROCESSED_BLOCK_EVENTS, contract.id)
                ?.toLongOrNull()
            ?: blockService.getBlockClosestTo(LocalDateTime.now().minusHours(24), contract.chain)

        // Get the latest block on the chain
        val latestBlock = blockService.getLatestBlock(contract.chain)

        // If we're already caught up, don't schedule a job
        if (lastProcessedBlock >= latestBlock) {
            logger.debug("Contract ${contract.address} events are up to date (block $lastProcessedBlock)")
            return
        }

        logger.info("Scheduling events job for contract ${contract.address} from block ${lastProcessedBlock + 1} to $latestBlock")

        // Create the job
        val metadata = mapOf(
            "startBlock" to (lastProcessedBlock + 1),
            "endBlock" to latestBlock
        )

        jobService.createJob(JobType.EVENTS, contract.id, metadata)
    }

    /**
     * Schedule a token transfer job if needed
     */
    private suspend fun scheduleTokenTransferRunupJob(contract: Contract) {
        val latestJob = jobService.getLatestCompletedJobByTypeAndContract(JobType.TOKEN_TRANSFERS, contract.id!!)

        // Get the last processed block from metadata or from the latest job
        val lastProcessedBlock = latestJob?.metadata?.get("endBlock")?.toString()?.toLongOrNull()
            ?: metadataRepository.getMetadatForContractId(
                MetadataType.LAST_PROCESSED_BLOCK_TRANSFER_EVENTS,
                contract.id
            )?.toLongOrNull()
            ?: blockService.getBlockClosestTo(LocalDateTime.now().minusHours(24), contract.chain)

        // Get the latest block on the chain
        val latestBlock = blockService.getLatestBlock(contract.chain)

        // If we're already caught up, don't schedule a job
        if (lastProcessedBlock >= latestBlock) {
            logger.debug("Contract ${contract.address} token transfers are up to date (block $lastProcessedBlock)")
            return
        }

        logger.info("Scheduling token transfer job for contract ${contract.address} from block ${lastProcessedBlock + 1} to $latestBlock")

        // Create the job
        val metadata = mapOf(
            "startBlock" to (lastProcessedBlock + 1),
            "endBlock" to latestBlock
        )

        jobService.createJob(JobType.TOKEN_TRANSFERS, contract.id, metadata)
    }

    /**
     * Schedule a job immediately for a specific contract and range
     */
    suspend fun scheduleImmediateJob(
        contract: Contract,
        jobType: JobType,
        startBlock: Long? = null,
        endBlock: Long? = null
    ): Int {
        val metadata = mutableMapOf<String, Any>()

        // If blocks are specified, use them
        if (startBlock != null) {
            metadata["startBlock"] = startBlock
        }

        if (endBlock != null) {
            metadata["endBlock"] = endBlock
        }

        // Otherwise use default logic to determine block range
        if (startBlock == null && endBlock == null) {
            val lastProcessedBlockKey = when (jobType) {
                JobType.RAW_INVOCATIONS -> MetadataType.LAST_PROCESSED_BLOCK_RAW_INVOCATIONS
                JobType.EVENTS -> MetadataType.LAST_PROCESSED_BLOCK_EVENTS
                JobType.TOKEN_TRANSFERS -> MetadataType.LAST_PROCESSED_BLOCK_TRANSFER_EVENTS
            }

            val lastProcessedBlock = metadataRepository
                .getMetadatForContractId(lastProcessedBlockKey, contract.id!!)
                ?.toLongOrNull()
                ?: blockService.getBlockClosestTo(LocalDateTime.now().minusHours(24), contract.chain)

            val latestBlock = blockService.getLatestBlock(contract.chain)

            metadata["startBlock"] = lastProcessedBlock + 1
            metadata["endBlock"] = latestBlock
        }

        logger.info("Scheduling immediate ${jobType.name} job for contract ${contract.address} with metadata: $metadata")
        return jobService.createJob(jobType, contract.id!!, metadata)
    }
}