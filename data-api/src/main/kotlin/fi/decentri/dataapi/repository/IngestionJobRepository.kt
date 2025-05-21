package fi.decentri.dataapi.repository

import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.ingestion.JobStatus
import fi.decentri.db.ingestion.Jobs
import org.jetbrains.exposed.sql.and
import kotlin.time.ExperimentalTime

/**
 * Repository for accessing ingestion metadata in the data-api module
 */
@ExperimentalTime
class IngestionJobRepository {

    suspend fun hasAnyCompletedJob(contractId: Int): Boolean {
        return dbQuery {
            Jobs.select(Jobs.contractId).where {
                (Jobs.status eq JobStatus.COMPLETED) and (Jobs.contractId eq contractId)
            }.limit(1).any()
        }
    }
}