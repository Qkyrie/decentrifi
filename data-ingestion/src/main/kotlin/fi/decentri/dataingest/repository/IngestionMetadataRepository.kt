package fi.decentri.dataingest.repository

import fi.decentri.dataingest.db.DatabaseFactory.dbQuery
import fi.decentri.dataingest.model.IngestionMetadata
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory

/**
 * Repository for managing ingestion metadata
 */
class IngestionMetadataRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    /**
     * Get the last processed block number from metadata
     */
    suspend fun getLastProcessedBlock(): Long {
        return dbQuery {
            IngestionMetadata.selectAll()
                .where { IngestionMetadata.key eq "last_processed_block" }
                .singleOrNull()
                ?.get(IngestionMetadata.value)?.toLongOrNull() ?: 0L
        }
    }
    
    /**
     * Update the last processed block number in metadata
     */
    suspend fun updateLastProcessedBlock(blockNumber: Long) {
        logger.debug("Updating last processed block to $blockNumber")
        dbQuery {
            IngestionMetadata.update({ IngestionMetadata.key eq "last_processed_block" }) {
                it[value] = blockNumber.toString()
            }
        }
    }
}
