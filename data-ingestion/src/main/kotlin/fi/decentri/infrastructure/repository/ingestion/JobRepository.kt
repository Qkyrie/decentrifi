package fi.decentri.infrastructure.repository.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.ingestion.JobStatus
import fi.decentri.db.ingestion.JobType
import fi.decentri.db.ingestion.Jobs
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlinx.serialization.json.*

/**
 * Repository for managing ingestion jobs
 */
class JobRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapper()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        isLenient = true
    }

    /**
     * Create a new job
     */
    suspend fun createJob(
        type: JobType,
        contractId: Int,
        metadata: Map<String, Any>
    ): Int = dbQuery {
        val now = Instant.now()

        Jobs.insert {
            it[Jobs.type] = type
            it[Jobs.status] = JobStatus.PENDING
            it[Jobs.contractId] = contractId
            it[Jobs.metadata] = Json.parseToJsonElement(
                objectMapper.writeValueAsString(metadata)
            )
            it[createdAt] = now
            it[updatedAt] = now
        }[Jobs.id]
    }

    /**
     * Update job status
     */
    suspend fun updateJobStatus(
        jobId: Int,
        status: JobStatus,
        errorMessage: String? = null
    ): Boolean = dbQuery {
        val now = Instant.now()

        val updates = mutableListOf<Pair<Column<*>, Any?>>()
        updates.add(Jobs.status to status)
        updates.add(Jobs.updatedAt to now)

        when (status) {
            JobStatus.RUNNING -> updates.add(Jobs.startedAt to now)
            JobStatus.COMPLETED -> updates.add(Jobs.completedAt to now)
            JobStatus.FAILED -> {
                updates.add(Jobs.completedAt to now)
                if (errorMessage != null) {
                    updates.add(Jobs.errorMessage to errorMessage)
                }
            }

            else -> {}
        }

        Jobs.update({ Jobs.id eq jobId }) {
            for ((column, value) in updates) {
                it[column as Column<Any>] = value as Any
            }
        } > 0
    }

    /**
     * Get jobs by status
     */
    suspend fun getJobsByStatus(status: JobStatus): List<JobData> = dbQuery {
        Jobs.select { Jobs.status eq status }
            .orderBy(Jobs.createdAt)
            .map { it.toJobData() }
    }

    /**
     * Get next pending job
     */
    suspend fun getNextPendingJob(): JobData? = dbQuery {
        Jobs.select { Jobs.status eq JobStatus.PENDING }
            .orderBy(Jobs.createdAt)
            .limit(1)
            .map { it.toJobData() }
            .firstOrNull()
    }

    /**
     * Get job by id
     */
    suspend fun getJobById(jobId: Int): JobData? = dbQuery {
        Jobs.select { Jobs.id eq jobId }
            .map { it.toJobData() }
            .firstOrNull()
    }

    /**
     * Get the most recent completed job by type and contract id
     */
    suspend fun getLatestCompletedJobByTypeAndContract(
        type: JobType,
        contractId: Int
    ): JobData? = dbQuery {
        Jobs.select {
            (Jobs.type eq type) and
                    (Jobs.contractId eq contractId) and
                    (Jobs.status eq JobStatus.COMPLETED)
        }
            .orderBy(Jobs.completedAt, SortOrder.DESC)
            .limit(1)
            .map { it.toJobData() }
            .firstOrNull()
    }

    /**
     * Update job metadata
     */
    suspend fun updateJobMetadata(
        jobId: Int,
        metadata: Map<String, Any>
    ): Boolean = dbQuery {
        Jobs.update({ Jobs.id eq jobId }) {
            it[Jobs.metadata] = Json.parseToJsonElement(
                objectMapper.writeValueAsString(metadata)
            )
            it[updatedAt] = Instant.now()
        } > 0
    }

    /**
     * Check if there are already running jobs for a contract
     */
    suspend fun hasRunningOrPendingJobsForContract(contractId: Int): Boolean = dbQuery {
        Jobs.select {
            (Jobs.contractId eq contractId) and
                    ((Jobs.status eq JobStatus.RUNNING) or (Jobs.status eq JobStatus.PENDING))
        }.count() > 0
    }

    /**
     * Map ResultRow to JobData
     */
    private fun ResultRow.toJobData(): JobData {
        val metadataStr = this[Jobs.metadata].toString()
        val metadataMap = json.decodeFromString<Map<String, String>>(metadataStr)
            .mapValues { it.value }

        return JobData(
            id = this[Jobs.id],
            type = this[Jobs.type],
            status = this[Jobs.status],
            contractId = this[Jobs.contractId],
            metadata = metadataMap,
            createdAt = this[Jobs.createdAt],
            updatedAt = this[Jobs.updatedAt],
            startedAt = this[Jobs.startedAt],
            completedAt = this[Jobs.completedAt],
            errorMessage = this[Jobs.errorMessage]
        )
    }
}

data class JobData(
    val id: Int,
    val type: JobType,
    val status: JobStatus,
    val contractId: Int,
    val metadata: Map<String, Any>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val errorMessage: String? = null
)
