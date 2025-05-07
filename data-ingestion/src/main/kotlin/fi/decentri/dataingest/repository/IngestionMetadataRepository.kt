package fi.decentri.dataingest.repository

import fi.decentri.dataingest.model.MetadataType
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.ingestion.IngestionMetadata
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory

/**
 * Repository for managing ingestion metadata
 */
class IngestionMetadataRepository {

    val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Get the last processed block number for a specific contract
     */
    suspend fun getMetadatForContractId(type: MetadataType, contractId: Int): String? {
        return dbQuery {
            IngestionMetadata.selectAll()
                .where { (IngestionMetadata.key eq type.key) and (IngestionMetadata.contractId eq contractId) }
                .singleOrNull()
                ?.get(IngestionMetadata.value)
        }
    }

    /**
     * Update the last processed block number for a specific contract
     */
    suspend fun updateMetadataForContractId(contractId: Int, type: MetadataType, theValue: String) {
        dbQuery {
            val count =
                IngestionMetadata.update({ (IngestionMetadata.key eq type.key) and (IngestionMetadata.contractId eq contractId) }) {
                    it[value] = theValue
                }
            logger.info("Updated ingestion metadata for contract $contractId and key ${type.key} with value $theValue")

            // If no row was updated, insert new row
            if (count == 0) {
                IngestionMetadata.insert {
                    it[key] = type.key
                    it[value] = theValue
                    it[this.contractId] = contractId
                }
                logger.info("Inserted new ingestion metadata for contract $contractId and key ${type.key}")
            }
        }
    }
}
