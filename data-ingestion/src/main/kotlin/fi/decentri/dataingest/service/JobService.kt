package fi.decentri.dataingest.service

import fi.decentri.db.ingestion.JobStatus
import fi.decentri.db.ingestion.JobType
import fi.decentri.infrastructure.repository.ingestion.JobData
import fi.decentri.infrastructure.repository.ingestion.JobRepository
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory

/**
 * Service for managing the lifecycle of ingestion jobs
 */
class JobService(
    private val jobRepository: JobRepository,
    private val coroutineScope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Create a new ingestion job
     */
    suspend fun createJob(
        type: JobType,
        contractId: Int,
        metadata: Map<String, Any>
    ): Int {
        logger.info("Creating job of type $type for contract ID $contractId")
        return jobRepository.createJob(type, contractId, metadata)
    }

    /**
     * Get a job by ID
     */
    suspend fun getJob(jobId: Int): JobData? {
        return jobRepository.getJobById(jobId)
    }

    /**
     * Get all jobs with the specified status
     */
    suspend fun getJobsByStatus(status: JobStatus): List<JobData> {
        return jobRepository.getJobsByStatus(status)
    }

    /**
     * Start a job
     */
    suspend fun startJob(jobId: Int): Boolean {
        logger.info("Starting job $jobId")
        return jobRepository.updateJobStatus(jobId, JobStatus.RUNNING)
    }

    /**
     * Complete a job successfully
     */
    suspend fun completeJob(jobId: Int): Boolean {
        logger.info("Completing job $jobId")
        return jobRepository.updateJobStatus(jobId, JobStatus.COMPLETED)
    }

    /**
     * Mark a job as failed
     */
    suspend fun failJob(jobId: Int, errorMessage: String): Boolean {
        logger.error("Job $jobId failed: $errorMessage")
        return jobRepository.updateJobStatus(jobId, JobStatus.FAILED, errorMessage)
    }

    /**
     * Update job metadata
     */
    suspend fun updateJobMetadata(jobId: Int, metadata: Map<String, Any>): Boolean {
        return jobRepository.updateJobMetadata(jobId, metadata)
    }

    /**
     * Get the next pending job
     */
    suspend fun getNextPendingJob(): JobData? {
        return jobRepository.getNextPendingJob()
    }

    /**
     * Get the most recent completed job of the specified type for a contract
     */
    suspend fun getLatestCompletedJobByTypeAndContract(
        type: JobType,
        contractId: Int
    ): JobData? {
        return jobRepository.getLatestCompletedJobByTypeAndContract(type, contractId)
    }

    /**
     * Check if there are running jobs for a contract
     */
    suspend fun hasRunningOrPendingJobsForContract(contractId: Int): Boolean {
        return jobRepository.hasRunningOrPendingJobsForContract(contractId)
    }
}