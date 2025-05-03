package fi.decentri.dataingest.repository

import fi.decentri.dataingest.db.DatabaseFactory.dbQuery
import fi.decentri.dataingest.model.IngestionMetadata
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory

/**
 * Repository for managing ingestion metadata
 */
class IngestionMetadataRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    /**
     * Get the last processed block number from global metadata
     */
    suspend fun getLastProcessedBlock(): Long {
        return dbQuery {
            IngestionMetadata.selectAll()
                .where { (IngestionMetadata.key eq "last_processed_block") and IngestionMetadata.contractId.isNull() }
                .singleOrNull()
                ?.get(IngestionMetadata.value)?.toLongOrNull() ?: 0L
        }
    }
    
    /**
     * Get the last processed block number for a specific contract
     */
    suspend fun getLastProcessedBlockForContract(contractId: Int): Long {
        return dbQuery {
            IngestionMetadata.selectAll()
                .where { (IngestionMetadata.key eq "last_processed_block") and (IngestionMetadata.contractId eq contractId) }
                .singleOrNull()
                ?.get(IngestionMetadata.value)?.toLongOrNull() ?: 0L
        }
    }
    
    /**
     * Update the global last processed block number in metadata
     */
    suspend fun updateLastProcessedBlock(blockNumber: Long) {
        logger.debug("Updating global last processed block to $blockNumber")
        dbQuery {
            val count = IngestionMetadata.update({ (IngestionMetadata.key eq "last_processed_block") and IngestionMetadata.contractId.isNull() }) {
                it[value] = blockNumber.toString()
            }
            
            // If no row was updated, insert new row
            if (count == 0) {
                IngestionMetadata.insert {
                    it[key] = "last_processed_block"
                    it[value] = blockNumber.toString()
                    it[contractId] = null
                }
            }
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
    
    /**
     * Get a metadata value by key
     */
    suspend fun getMetadataValue(key: String): String? {
        return dbQuery {
            IngestionMetadata.selectAll()
                .where { (IngestionMetadata.key eq key) and IngestionMetadata.contractId.isNull() }
                .singleOrNull()
                ?.get(IngestionMetadata.value)
        }
    }
    
    /**
     * Get a metadata value for a specific contract
     */
    suspend fun getMetadataValueForContract(contractId: Int, key: String): String? {
        return dbQuery {
            IngestionMetadata.selectAll()
                .where { (IngestionMetadata.key eq key) and (IngestionMetadata.contractId eq contractId) }
                .singleOrNull()
                ?.get(IngestionMetadata.value)
        }
    }
    
    /**
     * Set a metadata value
     */
    suspend fun setMetadataValue(key: String, value: String) {
        dbQuery {
            val count = IngestionMetadata.update({ (IngestionMetadata.key eq key) and IngestionMetadata.contractId.isNull() }) {
                it[this.value] = value
            }
            
            // If no row was updated, insert new row
            if (count == 0) {
                IngestionMetadata.insert {
                    it[this.key] = key
                    it[this.value] = value
                    it[contractId] = null
                }
            }
        }
    }
    
    /**
     * Set a metadata value for a specific contract
     */
    suspend fun setMetadataValueForContract(contractId: Int, key: String, value: String) {
        dbQuery {
            val count = IngestionMetadata.update({ (IngestionMetadata.key eq key) and (IngestionMetadata.contractId eq contractId) }) {
                it[this.value] = value
            }
            
            // If no row was updated, insert new row
            if (count == 0) {
                IngestionMetadata.insert {
                    it[this.key] = key
                    it[this.value] = value
                    it[this.contractId] = contractId
                }
            }
        }
    }
}
