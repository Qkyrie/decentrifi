package fi.decentri.db.rawinvocation

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object RawInvocations : Table("raw_invocations") {

    /* identity */
    val network   = varchar("network", 20)
    val txHash    = varchar("tx_hash", 66)
    val tracePath = varchar("trace_path", 40)          //  "", "0", "0.3.1" …

    /* navigation */
    val blockNumber    = long("block_number")
    val blockTimestamp = timestamp("block_timestamp")
    val contractAddr   = varchar("contract_address", 42)

    /* call meta */
    val depth      = integer("depth")
    val callType   = varchar("call_type", 20).nullable()   // call / delegatecall …
    val traceType  = varchar("trace_type", 20)             // "call", "create" …

    /* participants & value */
    val fromAddr   = varchar("from_addr", 42)
    val toAddr     = varchar("to_addr", 42).nullable()
    val valueWei   = decimal("value_wei", 78, 0)

    /* gas */
    val gas        = decimal("gas", 38, 0)
    val gasUsed    = decimal("gas_used", 38, 0)

    /* payload */
    val functionSelector = varchar("function_selector", 10).nullable()
    val input   = text("input").nullable()
    val output  = text("output").nullable()

    /** composite key = duplicate shield */
    override val primaryKey =
        PrimaryKey(network, txHash, tracePath, name = "pk_raw_invocations")
}