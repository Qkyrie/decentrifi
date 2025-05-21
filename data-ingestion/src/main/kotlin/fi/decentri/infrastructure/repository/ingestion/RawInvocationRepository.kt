package fi.decentri.infrastructure.repository.ingestion

import fi.decentri.application.ports.RawInvocationsPort
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.rawinvocation.RawInvocations
import org.jetbrains.exposed.sql.batchInsert
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.time.Instant

class RawInvocationRepository {

    suspend fun batchInsert(invocations: List<RawInvocationData>) {
        if (invocations.isEmpty()) return

        dbQuery {
            RawInvocations.batchInsert(invocations, ignore = true) { v ->
                this[RawInvocations.network]        = v.network
                this[RawInvocations.txHash]         = v.txHash
                this[RawInvocations.tracePath]      = v.tracePath

                this[RawInvocations.blockNumber]    = v.blockNumber
                this[RawInvocations.blockTimestamp] = v.blockTimestamp
                this[RawInvocations.contractAddr]   = v.contractAddress

                this[RawInvocations.depth]          = v.depth
                this[RawInvocations.callType]       = v.callType
                this[RawInvocations.traceType]      = v.traceType

                this[RawInvocations.fromAddr]       = v.from
                this[RawInvocations.toAddr]         = v.to
                this[RawInvocations.valueWei]       = v.valueWei.toBigDecimal()

                this[RawInvocations.gas]            = v.gas.toBigDecimal()
                this[RawInvocations.gasUsed]        = v.gasUsed.toBigDecimal()

                this[RawInvocations.functionSelector] = v.functionSelector
                this[RawInvocations.input]            = v.input
                this[RawInvocations.output]           = v.output
            }
        }
    }
}

data class RawInvocationData(
    val network: String,
    val contractAddress: String,
    val blockNumber: Long,
    val blockTimestamp: Instant,
    val txHash: String,
    val tracePath: String,          //  "", "0", "0.3.1"
    val depth: Int,
    val callType: String?,
    val traceType: String,
    val from: String,
    val to: String?,
    val valueWei: BigInteger,
    val gas: BigInteger,
    val gasUsed: BigInteger,
    val functionSelector: String?,
    val input: String?,
    val output: String?
)