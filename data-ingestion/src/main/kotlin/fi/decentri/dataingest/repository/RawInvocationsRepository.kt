package fi.decentri.dataingest.repository

import fi.decentri.dataingest.model.RawInvocations
import fi.decentri.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Repository for managing raw contract invocations data
 */
class RawInvocationsRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Insert a single raw invocation record
     */
    suspend fun insert(
        network: String,
        contractAddress: String,
        blockNumber: Long,
        blockTimestamp: Instant,
        txHash: String,
        fromAddress: String,
        functionSelector: String,
        functionName: String?,
        inputArgs: Map<String, Any>,
        status: Boolean,
        gasUsed: Long
    ): Int {
        return dbQuery {
            RawInvocations.insert {
                it[RawInvocations.network] = network
                it[RawInvocations.contractAddress] = contractAddress
                it[RawInvocations.blockNumber] = blockNumber
                it[RawInvocations.txHash] = txHash
                it[RawInvocations.fromAddress] = fromAddress
                it[RawInvocations.functionSelector] = functionSelector
                it[RawInvocations.status] = status
                it[RawInvocations.gasUsed] = gasUsed
            }[RawInvocations.id]
        }
    }

    /**
     * Batch insert multiple raw invocation records
     */
    suspend fun batchInsert(invocations: List<RawInvocationData>) {
        if (invocations.isEmpty()) return

        logger.debug("Batch inserting ${invocations.size} invocations")
        dbQuery {
            RawInvocations.batchInsert(invocations) { data ->
                this[RawInvocations.network] = data.network
                this[RawInvocations.contractAddress] = data.contractAddress
                this[RawInvocations.blockNumber] = data.blockNumber
                this[RawInvocations.blockTimestamp] = data.blockTimestamp
                this[RawInvocations.txHash] = data.txHash
                this[RawInvocations.fromAddress] = data.fromAddress
                this[RawInvocations.functionSelector] = data.functionSelector
                this[RawInvocations.status] = data.status
                this[RawInvocations.gasUsed] = data.gasUsed
            }
        }
    }
}

/**
 * Data class for raw invocation batch operations
 */
data class RawInvocationData(
    val network: String,
    val contractAddress: String,
    val blockNumber: Long,
    val blockTimestamp: Instant,
    val txHash: String,
    val fromAddress: String,
    val functionSelector: String,
    val inputArgs: Map<String, Any>,
    val status: Boolean,
    val gasUsed: Long
)
