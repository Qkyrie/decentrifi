package fi.decentri.dataingest.repository

import fi.decentri.dataingest.model.IngestionMetadata
import fi.decentri.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory

/**
 * Repository for managing ingestion metadata
 */
class IngestionMetadataRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    /**
     * Get the last processed block number for a specific contract
     */
    suspend fun getLastProcessedBlockForContract(contractId: Int): Long? {
        return dbQuery {
            IngestionMetadata.selectAll()
                .where { (IngestionMetadata.key eq "last_processed_block") and (IngestionMetadata.contractId eq contractId) }
                .singleOrNull()
                ?.get(IngestionMetadata.value)?.toLongOrNull()
        }
    }

    /**
     * Update the last processed block number for a specific contract
     */
    suspend fun updateLastProcessedBlockForContract(contractId: Int, blockNumber: Long) {
        logger.debug("Updating last processed block for contract $contractId to $blockNumber")
        dbQuery {
            val count = IngestionMetadata.update({ (IngestionMetadata.key eq "last_processed_block") and (IngestionMetadata.contractId eq contractId) }) {
                it[value] = blockNumber.toString()
            }
            
            // If no row was updated, insert new row
            if (count == 0) {
                IngestionMetadata.insert {
                    it[key] = "last_processed_block"
                    it[value] = blockNumber.toString()
                    it[this.contractId] = contractId
                }
            }
        }
    }
}
