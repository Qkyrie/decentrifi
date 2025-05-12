package fi.decentri.dataingest.repository

import fi.decentri.application.ports.RawInvocationsPort
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.rawinvocation.RawInvocations
import org.jetbrains.exposed.sql.batchInsert
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Repository for managing raw contract invocations data
 */
class RawInvocationRepository : RawInvocationsPort {
    private val logger = LoggerFactory.getLogger(this::class.java)

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
    val gasUsed: Long
)
