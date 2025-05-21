package fi.decentri.dataingest.service

import fi.decentri.application.usecases.EventIngestor
import fi.decentri.application.usecases.RawInvocationIngestor
import fi.decentri.application.usecases.TokenTransferIngestor
import fi.decentri.dataingest.model.Contract
import fi.decentri.db.ingestion.JobType
import fi.decentri.infrastructure.repository.ingestion.JobData
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

/**
 * Service responsible for processing ingestion jobs
 */
@OptIn(ExperimentalTime::class)
class JobProcessorService(
    private val jobService: JobService,
    private val contractsService: ContractsService,
    private val rawInvocationIngestor: RawInvocationIngestor,
    private val eventIngestor: EventIngestor,
    private val tokenTransferIngestor: TokenTransferIngestor,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Start the job processor
     */
    suspend fun run(): Job = coroutineScope {
        launch {
            logger.info("Starting job processor service")
            processJobs()
        }
    }

    /**
     * Main job processing loop
     */
    private suspend fun processJobs() {
        try {
            // Get the next pending job
            val job = jobService.getNextPendingJob() ?: return

            logger.info("Processing job ${job.id} of type ${job.type} for contract ${job.contractId}")
            processJob(job)
            delay(1000)
            processJobs()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.error("Error in job processor: ${e.message}", e)
            delay(10000) // Wait a bit longer on error
        }
    }

    /**
     * Process a single job
     */
    private suspend fun processJob(job: JobData) {
        try {
            // Mark job as running
            jobService.startJob(job.id)

            // Get the contract for this job
            val contract = contractsService.getContract(job.contractId)
                ?: throw IllegalStateException("Contract with ID ${job.contractId} not found")

            // Process based on job type
            when (job.type) {
                JobType.RAW_INVOCATIONS -> processRawInvocationsJob(job, contract)
                JobType.EVENTS -> processEventsJob(job, contract)
                JobType.TOKEN_TRANSFERS -> processTokenTransfersJob(job, contract)
            }

            // Mark job as completed
            jobService.completeJob(job.id)
            logger.info("Job ${job.id} completed successfully")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.error("Error processing job ${job.id}: ${e.message}", e)
            jobService.failJob(job.id, e.message ?: "Unknown error")
        }
    }

    /**
     * Process a raw invocations job
     */
    private suspend fun processRawInvocationsJob(job: JobData, contract: Contract) {
        logger.info("Processing raw invocations job for contract ${contract.address}")
        rawInvocationIngestor.ingest(
            contract = contract,
            startBlock = job.getStartBlock(),
            endBlock = job.getEndBlock(),
        )
    }

    /**
     * Process an events job
     */
    private suspend fun processEventsJob(job: JobData, contract: Contract) {
        logger.info("Processing events job for contract ${contract.address}")

        eventIngestor.ingest(
            contract = contract,
            startBlock = job.getStartBlock(),
            endBlock = job.getEndBlock(),
        )
    }

    /**
     * Process a token transfers job
     */
    private suspend fun processTokenTransfersJob(job: JobData, contract: Contract) {
        logger.info("Processing token transfers job for contract ${contract.address}")
        tokenTransferIngestor.ingest(
            contract = contract,
            startBlock = job.getStartBlock(),
            endBlock = job.getEndBlock(),
        )
    }

    fun JobData.getStartBlock(): Long {
        return metadata["startBlock"]?.toString()?.toLongOrNull()
            ?: error("job metadata must contain startBlock key with a long value, got: ${metadata["startBlock"]}")
    }

    fun JobData.getEndBlock(): Long {
        return metadata["endBlock"]?.toString()?.toLongOrNull()
            ?: error("job metadata must contain endBlock key with a long value, got: ${metadata["endBlock"]}")
    }
}