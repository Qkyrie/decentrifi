package fi.decentri.dataingest.repository

import fi.decentri.dataingest.model.IngestionMetadata
import fi.decentri.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory

/**
 * Repository for managing ingestion metadata
 */
class IngestionMetadataRepository {

    val logger = LoggerFactory.getLogger(this::class.java)

    val types = listOf(
        "last_processed_block_raw_invocations",
        "last_processed_block_events",
    )

    /**
     * Get the last processed block number for a specific contract
     */
    suspend fun getMetadatForContractId(type: String, contractId: Int): String? {
        return dbQuery {
            IngestionMetadata.selectAll()
                .where { (IngestionMetadata.key eq type) and (IngestionMetadata.contractId eq contractId) }
                .singleOrNull()
                ?.get(IngestionMetadata.value)
        }
    }

    /**
     * Update the last processed block number for a specific contract
     */
    suspend fun updateMetadataForContractId(contractId: Int, theKey: String, theValue: String) {
        dbQuery {
            val count =
                IngestionMetadata.update({ (IngestionMetadata.key eq theKey) and (IngestionMetadata.contractId eq contractId) }) {
                    it[value] = theValue
                }
            logger.info("Updated ingestion metadata for contract $contractId and key $theKey with value $theValue")

            // If no row was updated, insert new row
            if (count == 0) {
                IngestionMetadata.insert {
                    it[key] = theKey
                    it[value] = theValue
                    it[this.contractId] = contractId
                }
                logger.info("Inserted new ingestion metadata for contract $contractId and key $theKey")
            }
        }
    }
}
