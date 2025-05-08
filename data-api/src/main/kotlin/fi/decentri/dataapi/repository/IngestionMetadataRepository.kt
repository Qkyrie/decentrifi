package fi.decentri.dataapi.repository

import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.ingestion.IngestionMetadata
import kotlin.time.ExperimentalTime

/**
 * Repository for accessing ingestion metadata in the data-api module
 */
@ExperimentalTime
class IngestionMetadataRepository {

    /**
     * Check if any processing metadata exists for a contract
     */
    suspend fun hasAnyMetadataForContract(contractId: Int): Boolean {
        return dbQuery {
            IngestionMetadata.select(IngestionMetadata.contractId).where {
                IngestionMetadata.contractId eq contractId
            }.limit(1).any()
        }
    }
}